package fiji.plugin.mamut.threedviewer;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.java3d.BadTransformException;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Color4f;
import org.scijava.vecmath.Point4d;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.util.gui.GenericDialogPlus;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

public class SpotDisplayer3D extends AbstractTrackMateModelView
{

	static final String KEY = "3DVIEWER";
	
	private static final Font BIG_FONT = new Font( "Arial", Font.PLAIN, 14 );	

	public static final int DEFAULT_RESAMPLING_FACTOR = 4;
	
	// public static final int DEFAULT_THRESHOLD = 50;

	private static final boolean DEBUG = false;

	private static final String TRACK_CONTENT_NAME = "Tracks";

	private static final String SPOT_CONTENT_NAME = "Spots";

	private TreeMap< Integer, SpotGroupNode< Spot >> blobs;

	private TrackDisplayNode trackNode;

	private Content spotContent;

	private Content trackContent;

	private final Image3DUniverse universe;

	// For highlighting
	private ArrayList< Spot > previousSpotHighlight;

	private HashMap< Spot, Color3f > previousColorHighlight;

	private HashMap< Spot, Integer > previousFrameHighlight;

	private TreeMap< Integer, ContentInstant > contentAllFrames;
	
	// Options for optimizing for large datasets
	private static final int OPTION_PROCESS_SPOTS = 0;
	private static final int OPTION_PROCESS_TEXT = 1;
	private static final int OPTION_PROCESS_TRACKS = 2;
	private static final int OPTION_USE_ICOSPHERES = 3;
	private static final int OPTION_MAX = 4;	
	protected final static boolean[] options = new boolean[] {true,false,false,true};
	
	/*
	private void debugDialog( String message )
	{
		final GenericDialogPlus dialog = new GenericDialogPlus( "DEBUG" );
		dialog.addMessage( message, BIG_FONT );	
	}
	*/
	
	private void showOptionsDialog()
	{
		final GenericDialogPlus dialog = new GenericDialogPlus( "Optimization Options" );

		dialog.addMessage( "3D Viewer optimizations for large dataset rendering...", BIG_FONT );

		dialog.addCheckbox( "Process spots", options[OPTION_PROCESS_SPOTS] );
		dialog.addCheckbox( "Process text", options[OPTION_PROCESS_TEXT] );
		dialog.addCheckbox( "Process tracks", options[OPTION_PROCESS_TRACKS] );
		dialog.addCheckbox( "Use icospheres for spots (uses less RAM)", options[OPTION_USE_ICOSPHERES] );
		
		dialog.showDialog();

		if ( dialog.wasCanceled() ) { return; }

		options[OPTION_PROCESS_SPOTS] = dialog.getNextBoolean();
		options[OPTION_PROCESS_TEXT] = dialog.getNextBoolean();
		options[OPTION_PROCESS_TRACKS] = dialog.getNextBoolean();
		options[OPTION_USE_ICOSPHERES] = dialog.getNextBoolean();
	}	

	public SpotDisplayer3D( final Model model, final SelectionModel selectionModel, final Image3DUniverse universe, final DisplaySettings ds )
	{
		super( model, selectionModel, ds );
		
		showOptionsDialog();		
		this.universe = universe;
		setModel( model );
	}

	/*
	 * OVERRIDDEN METHODS
	 */

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		if ( DEBUG )
		{
			System.out.println( "[SpotDisplayer3D: modelChanged() called with event ID: " + event.getEventID() );
			System.out.println( event );
		}

		switch ( event.getEventID() )
		{

		case ModelChangeEvent.SPOTS_COMPUTED:
			makeSpotContent();
			break;

		case ModelChangeEvent.SPOTS_FILTERED:
			for ( final int frame : blobs.keySet() )
			{
				final SpotGroupNode< Spot > frameBlobs = blobs.get( frame );
				for ( final Iterator< Spot > it = model.getSpots().iterator( frame, false ); it.hasNext(); )
				{
					final Spot spot = it.next();
					final boolean visible = spot.getFeature( SpotCollection.VISIBILITY ).compareTo( SpotCollection.ZERO ) > 0;
					frameBlobs.setVisible( spot, visible );
				}
			}
			break;

		case ModelChangeEvent.TRACKS_COMPUTED:
			final boolean processTracks = options[OPTION_PROCESS_TRACKS];
			trackContent = makeTrackContent(processTracks);
			universe.removeContent( TRACK_CONTENT_NAME );
			if (processTracks)
				universe.addContent( trackContent );
			break;

		case ModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
			updateTrackColors();
			trackNode.setTrackVisible( model.getTrackModel().trackIDs( true ) );
			break;

		case ModelChangeEvent.MODEL_MODIFIED:
		{
			/*
			 * We do not do anything. I could not find a good way to dynamically
			 * change the content of a 3D viewer content. So the 3D viewer just
			 * shows a snapshot of the TrackMate model when it was launched, and
			 * is not kept in sync with modifications afterwards.
			 */
			break;
		}

		default:
		{
			System.err.println( "[SpotDisplayer3D] Unknown event ID: " + event.getEventID() );
		}
		}
	}

	@Override
	public void selectionChanged( final SelectionChangeEvent event )
	{
		// Highlight edges.
		trackNode.setSelection( selectionModel.getEdgeSelection() );
		trackNode.refresh();
		// Highlight spots.
		displaySpotSelection( displaySettings.getTrackDisplayMode() == TrackDisplayMode.SELECTION_ONLY );
		// Center on last spot
		super.selectionChanged( event );
	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		universe.showTimepoint( frame );
	}

	@Override
	public void refresh()
	{
		if ( null != trackNode )
			trackNode.refresh();
	}

	@Override
	public void render()
	{
		//if ( DEBUG )
			System.out.println( "[SpotDisplayer3D] Call to render()." );

		updateRadiuses();
		updateSpotColors();
		spotContent.setVisible( displaySettings.isSpotVisible() );
		
		//System.out.println( "[SpotDisplayer3D] Call to render(A)." );
		
		if ( null != trackContent )
		{
			trackContent.setVisible( displaySettings.isTrackVisible() );
			//trackNode.setTrackDisplayMode( displaySettings.getTrackDisplayMode() );
			//trackNode.setTrackDisplayDepth( displaySettings.getFadeTrackRange() );
			updateTrackColors();
			//System.out.println( "[SpotDisplayer3D] Call to render(B)." );
			trackNode.refresh();
			universe.updateStartAndEndTime( blobs.firstKey(), blobs.lastKey() );
			universe.updateTimelineGUI();
		}
		
		//System.out.println( "[SpotDisplayer3D] Call to render(C)." );
	}
/*
	@Override
	public void setDisplaySettings( final String key, final Object value )
	{
		super.setDisplaySettings( key, value );
		// Treat change of radius
		if ( key == KEY_SPOT_RADIUS_RATIO )
		{
			updateRadiuses();
		}
		else if ( key == KEY_SPOT_COLORING )
		{
			updateSpotColors();
		}
		else if ( key == KEY_TRACK_COLORING )
		{
			updateTrackColors();
		}
		else if ( key == KEY_DISPLAY_SPOT_NAMES )
		{
			for ( final int frame : blobs.keySet() )
			{
				blobs.get( frame ).setShowLabels( ( Boolean ) value );
			}
		}
		else if ( key == KEY_SPOTS_VISIBLE )
		{
			spotContent.setVisible( ( Boolean ) value );
		}
		else if ( key == KEY_TRACKS_VISIBLE && null != trackContent )
		{
			trackContent.setVisible( ( Boolean ) value );
		}
		else if ( key == KEY_TRACK_DISPLAY_MODE && null != trackNode )
		{
			trackNode.setTrackDisplayMode( ( Integer ) value );
			displaySpotSelection( ( Integer ) value == TrackMateModelView.TRACK_DISPLAY_MODE_SELECTION_ONLY );
		}
		else if ( key == KEY_TRACK_DISPLAY_DEPTH && null != trackNode )
		{
			trackNode.setTrackDisplayDepth( ( Integer ) value );
		}
	}
*/
	@Override
	public void clear()
	{
		universe.removeContent( SPOT_CONTENT_NAME );
		universe.removeContent( TRACK_CONTENT_NAME );
	}

	/*
	 * PRIVATE METHODS
	 */

	private void setModel( final Model model )
	{
		final boolean processTracks = options[OPTION_PROCESS_TRACKS];
		if ( model.getSpots() != null )
		{
			makeSpotContent();
		}
		if ( model.getTrackModel().nTracks( true ) > 0 )
		{
			trackContent = makeTrackContent( processTracks );
			universe.removeContent( TRACK_CONTENT_NAME );
			
			if ( processTracks )
			{
				universe.addContentLater( trackContent );
			}
		}
	}

	private Content makeTrackContent( boolean processTracks )
	{
		// Prepare tracks instant
		trackNode = new TrackDisplayNode( model, displaySettings );
		if ( processTracks )
			universe.addTimelapseListener( trackNode );

		// Pass tracks instant to all instants
		final TreeMap< Integer, ContentInstant > instants = new TreeMap< >();
		final ContentInstant trackCI = new ContentInstant( "Tracks_all_frames" );
		trackCI.display( trackNode );
		instants.put( 0, trackCI );
		final Content tc = new Content( TRACK_CONTENT_NAME, instants );
		tc.setShowAllTimepoints( true );
		tc.showCoordinateSystem( false );
		return tc;
	}

	private void makeSpotContent()
	{

		blobs = new TreeMap<>();
		contentAllFrames = new TreeMap< >();
		final double radiusRatio = displaySettings.getSpotDisplayRadius();
		final SpotCollection spots = model.getSpots();
		//@SuppressWarnings( "unchecked" )
		//final FeatureColorGenerator< Spot > spotColorGenerator = ( FeatureColorGenerator< Spot > ) displaySettings.get( KEY_SPOT_COLORING );
		final FeatureColorGenerator< Spot > spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, displaySettings );

		for ( final int frame : spots.keySet() )
		{
			if ( spots.getNSpots( frame, false ) == 0 )
			{
				continue; // Do not create content for empty frames
			}
			buildFrameContent( spots, frame, radiusRatio, spotColorGenerator );
		}

		spotContent = new Content( SPOT_CONTENT_NAME, contentAllFrames );
		spotContent.showCoordinateSystem( false );
		universe.removeContent( SPOT_CONTENT_NAME );
		universe.addContentLater( spotContent );
	}

	private void buildFrameContent( final SpotCollection spots, final Integer frame, final double radiusRatio, final FeatureColorGenerator< Spot > spotColorGenerator )
	{
		final Map< Spot, Point4d > centers = new HashMap< >( spots.getNSpots( frame, false ) );
		final Map< Spot, Color4f > colors = new HashMap< >( spots.getNSpots( frame, false ) );
		final double[] coords = new double[ 3 ];

		for ( final Iterator< Spot > it = spots.iterator( frame, false ); it.hasNext(); )
		{
			final Spot spot = it.next();
			localize( spot, coords );
			final Double radius = spot.getFeature( Spot.RADIUS );
			final double[] pos = new double[] { coords[ 0 ], coords[ 1 ], coords[ 2 ], radius * radiusRatio };
			centers.put( spot, new Point4d( pos ) );
			final Color4f col = new Color4f( spotColorGenerator.color( spot ) );
			col.w = 0f;
			colors.put( spot, col );
		}
		final SpotGroupNode< Spot > blobGroup = new SpotGroupNode< >( centers, colors, options[OPTION_PROCESS_SPOTS], options[OPTION_PROCESS_TEXT], options[OPTION_USE_ICOSPHERES] );
		final ContentInstant contentThisFrame = new ContentInstant( "Spots_frame_" + frame );

		try
		{
			contentThisFrame.display( blobGroup );
		}
		catch ( final BadTransformException bte )
		{
			System.err.println( "Bad content for frame " + frame + ". Generated an exception:\n" + bte.getLocalizedMessage() + "\nContent was:\n" + blobGroup.toString() );
		}

		// Set visibility:
		if ( spots.getNSpots( frame, true ) > 0 )
		{
			blobGroup.setVisible( spots.iterable( frame, true ) );
		}

		contentAllFrames.put( frame, contentThisFrame );
		blobs.put( frame, blobGroup );
	}

	private void updateRadiuses()
	{
		final double radiusRatio = displaySettings.getSpotDisplayRadius();

		for ( final int frame : blobs.keySet() )
		{
			final SpotGroupNode< Spot > spotGroup = blobs.get( frame );
			for ( final Iterator< Spot > iterator = model.getSpots().iterator( frame, false ); iterator.hasNext(); )
			{
				final Spot spot = iterator.next();
				spotGroup.setRadius( spot, radiusRatio * spot.getFeature( Spot.RADIUS ) );
			}
		}
	}

	private void updateSpotColors()
	{
		//@SuppressWarnings( "unchecked" )
		//final FeatureColorGenerator< Spot > spotColorGenerator = ( FeatureColorGenerator< Spot > ) displaySettings.get( KEY_SPOT_COLORING );
		final FeatureColorGenerator< Spot > spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, displaySettings );

		for ( final int frame : blobs.keySet() )
		{
			final SpotGroupNode< Spot > spotGroup = blobs.get( frame );
			for ( final Iterator< Spot > iterator = model.getSpots().iterator( frame, false ); iterator.hasNext(); )
			{
				final Spot spot = iterator.next();
				spotGroup.setColor( spot, new Color3f( spotColorGenerator.color( spot ) ) );
			}
		}
	}

	private void updateTrackColors()
	{
		//final TrackColorGenerator colorGenerator = ( TrackColorGenerator ) displaySettings.get( KEY_TRACK_COLORING );
		final FeatureColorGenerator< DefaultWeightedEdge > colorGenerator = FeatureUtils.createTrackColorGenerator( model, displaySettings );

		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			//colorGenerator.setCurrentTrackID( trackID );
			for ( final DefaultWeightedEdge edge : model.getTrackModel().trackEdges( trackID ) )
			{
				final Color color = colorGenerator.color( edge );
				trackNode.setColor( edge, color );
			}
		}
	}

	private void highlightSpots( final Collection< Spot > spots )
	{
		// Restore previous display settings for previously highlighted spot
		if ( null != previousSpotHighlight )
			for ( final Spot spot : previousSpotHighlight )
			{
				final Integer frame = previousFrameHighlight.get( spot );
				if ( null != frame )
				{
					final SpotGroupNode< Spot > spotGroupNode = blobs.get( frame );
					if ( null != spotGroupNode )
					{
						spotGroupNode.setColor( spot, previousColorHighlight.get( spot ) );
					}
				}
			}

		/*
		 * Don't color spot selection in the highlight color if we are
		 * displaying selection only.
		 */

		final TrackDisplayMode trackDisplayMode = displaySettings.getTrackDisplayMode();
		if ( trackDisplayMode == TrackDisplayMode.SELECTION_ONLY )
			return;

		/*
		 * Store previous color value and color the spot selection with the
		 * highlight color.
		 */

		previousSpotHighlight = new ArrayList< >( spots.size() );
		previousColorHighlight = new HashMap< >( spots.size() );
		previousFrameHighlight = new HashMap< >( spots.size() );

		final Color3f highlightColor = new Color3f( displaySettings.getHighlightColor() );
		for ( final Spot spot : spots )
		{
			final int frame = spot.getFeature( Spot.FRAME ).intValue();
			// Store current settings
			previousSpotHighlight.add( spot );
			final SpotGroupNode< Spot > spotGroupNode = blobs.get( frame );
			if ( null != spotGroupNode )
			{
				previousColorHighlight.put( spot, spotGroupNode.getColor3f( spot ) );
				previousFrameHighlight.put( spot, frame );
				// Update target spot display
				blobs.get( frame ).setColor( spot, highlightColor );
			}
		}
	}
	
	/**
	 * Store the x, y, z coordinates of the specified spot in the first 3
	 * elements of the specified double array.
	 */
	private static final void localize( final Spot spot, final double[] coords )
	{
		coords[ 0 ] = spot.getFeature( Spot.POSITION_X ).doubleValue();
		coords[ 1 ] = spot.getFeature( Spot.POSITION_Y ).doubleValue();
		coords[ 2 ] = spot.getFeature( Spot.POSITION_Z ).doubleValue();
	}

	/**
	 * Changes the visibility of the displayed spot.
	 *
	 * @param onlySpotSelection
	 *            If <code>true</code>, we display on the spots in the
	 *            selection. Otherwise we display all spots marked as visible.
	 */
	private void displaySpotSelection( final boolean onlySpotSelection )
	{
		final Set< Spot > spotSelection = selectionModel.getSpotSelection();
		if ( onlySpotSelection )
		{
			if ( spotSelection.isEmpty() )
			{
				for ( final Integer frame : blobs.keySet() )
				{
					blobs.get( frame ).setVisible( false );
				}
				return;
			}

			// Sort spots in selection per frame.
			final HashMap< Integer, ArrayList< Spot > > spotsPerFrame = new HashMap< >( blobs.size() );
			for ( final Integer frame : blobs.keySet() )
			{
				final ArrayList< Spot > spots = new ArrayList< >();
				spotsPerFrame.put( frame, spots );
			}

			for ( final Spot spot : spotSelection )
			{
				final int frame = spot.getFeature( Spot.FRAME ).intValue();
				final ArrayList< Spot > spots = spotsPerFrame.get( Integer.valueOf( frame ) );
				spots.add( spot );
			}

			// Mark then as visible, the others as invisible.
			for ( final Integer frame : spotsPerFrame.keySet() )
			{
				blobs.get( frame ).setVisible( spotsPerFrame.get( frame ) );
			}

			// Restore proper color.
			updateSpotColors();
			updateTrackColors();
		}
		else
		{
			// Make all visible spots visible here.
			for ( final int frame : blobs.keySet() )
			{
				final Iterable< Spot > spots = model.getSpots().iterable( frame, true );
				blobs.get( frame ).setVisible( spots );
			}
			highlightSpots( spotSelection );
		}
	}

	@Override
	public String getKey()
	{
		return KEY;
	}
}
