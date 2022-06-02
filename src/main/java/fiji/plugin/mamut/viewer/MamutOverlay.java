/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2022 MaMuT development team.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.mamut.viewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import bdv.viewer.ViewerState;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import net.imglib2.realtransform.AffineTransform3D;

public class MamutOverlay
{

	/** Constants for re-use, to create ortho projections of triangle vectors. */
	protected static final float SINE_120 = (float) Math.sin(Math.toRadians(120));
	protected static final float COSINE_120 = (float) Math.cos(Math.toRadians(120));
	protected static final float SINE_NEG120 = (float) Math.sin(Math.toRadians(-120));
	protected static final float COSINE_NEG120 = (float) Math.cos(Math.toRadians(-120));

	/** The viewer state. */
	protected ViewerState state;

	/** The transform for the viewer current viewpoint. */
	protected final AffineTransform3D transform = new AffineTransform3D();

	/** The model to point on this overlay. */
	protected final Model model;

	/** The viewer in which this overlay is painted. */
	protected final MamutViewer viewer;

	/** The selection model. Items belonging to it will be highlighted. */
	protected final SelectionModel selectionModel;

	protected final DisplaySettings ds;

	public MamutOverlay( final Model model, final SelectionModel selectionModel, final MamutViewer viewer, final DisplaySettings ds )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.viewer = viewer;
		this.ds = ds;
	}

	public void paint( final Graphics2D g )
	{

		/*
		 * Collect current view.
		 */
		state.getViewerTransform( transform );

		/*
		 * Common display settings.
		 */
		final boolean doLimitDrawingDepth = ds.isZDrawingDepthLimited();
		final double drawingDepth = ds.getZDrawingDepth();
		final TrackDisplayMode trackDisplayMode = ds.getTrackDisplayMode();
		final int trackDisplayDepth = ds.getFadeTrackRange();
		final boolean tracksVisible = ds.isTrackVisible();
		final double radiusRatio = ds.getSpotDisplayRadius();
		final boolean drawCellTriangles = ( Boolean ) ( ds.isSpotVisible() && tracksVisible && trackDisplayDepth > 0 && trackDisplayDepth < 1_000_000_000 && trackDisplayMode == TrackDisplayMode.LOCAL );
		final boolean doDisplayNames = ds.isSpotShowName();
		final Stroke normalStroke = new BasicStroke( ( float ) ds.getLineThickness() );
		final Stroke selectionStroke = new BasicStroke( ( float ) ds.getSelectionLineThickness() );
		final Stroke halfSelectionStroke = new BasicStroke( ( float ) ( ds.getSelectionLineThickness() / 2 ) );
		final FeatureColorGenerator< Spot > spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, ds );
		final FeatureColorGenerator< DefaultWeightedEdge > trackColorGenerator = FeatureUtils.createTrackColorGenerator( model, ds );
		
		/*
		 * Compute scale
		 */
		final double vx = transform.get( 0, 0 );
		final double vy = transform.get( 1, 0 );
		final double vz = transform.get( 2, 0 );
		final double transformScale = Math.sqrt( vx * vx + vy * vy + vz * vz );

		/*
		 * Draw spots.
		 */

		if ( ds.isSpotVisible() && !(drawCellTriangles) )
		{
			g.setFont( ds.getFont() );

			final Iterable< Spot > spots;
			final int frame = state.getCurrentTimepoint();
			if ( trackDisplayMode != TrackDisplayMode.SELECTION_ONLY )
			{
				spots = model.getSpots().iterable( frame, true );
			}
			else
			{
				final ArrayList< Spot > tmp = new ArrayList<>();
				for ( final Spot spot : selectionModel.getSpotSelection() )
				{
					if ( spot.getFeature( Spot.FRAME ).intValue() == frame )
						tmp.add( spot );
				}
				spots = tmp;
			}

			for ( final Spot spot : spots )
			{

				Stroke stroke = normalStroke;
				boolean forceDraw = !doLimitDrawingDepth;
				final Color color;
				if ( selectionModel.getSpotSelection().contains( spot ) && trackDisplayMode != TrackDisplayMode.SELECTION_ONLY )
				{
					forceDraw = true; // Selection is drawn unconditionally.
					color = ds.getHighlightColor();
					stroke = selectionStroke;
				}
				else
				{
					color = spotColorGenerator.color( spot );

				}
				g.setColor( color );
				g.setStroke( stroke );

				final double x = spot.getFeature( Spot.POSITION_X );
				final double y = spot.getFeature( Spot.POSITION_Y );
				final double z = spot.getFeature( Spot.POSITION_Z );
				final double radius = spot.getFeature( Spot.RADIUS );

				final double[] globalCoords = new double[] { x, y, z };
				final double[] viewerCoords = new double[ 3 ];
				transform.apply( globalCoords, viewerCoords );

				final double rad = radius * transformScale * radiusRatio;
				final double zv = viewerCoords[ 2 ];
				final double dz2 = zv * zv;

				if ( !forceDraw && Math.abs( zv ) > drawingDepth )
					continue;

				if ( dz2 < rad * rad )
				{

					final double arad = Math.sqrt( rad * rad - dz2 );
					g.drawOval( ( int ) ( viewerCoords[ 0 ] - arad ), ( int ) ( viewerCoords[ 1 ] - arad ), ( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );

					if ( doDisplayNames )
					{
						final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
						final int ty = ( int ) viewerCoords[ 1 ];
						g.drawString( spot.getName(), tx, ty );
					}

				}
				else
				{
					g.fillOval( ( int ) viewerCoords[ 0 ] - 2, ( int ) viewerCoords[ 1 ] - 2, 4, 4 );
				}
			}

		}

		/*
		 * Draw edges
		 */

		if ( tracksVisible && model.getTrackModel().nTracks( false ) > 0 )
		{

			// Save graphic device original settings
			final Composite originalComposite = g.getComposite();
			final Stroke originalStroke = g.getStroke();
			final Color originalColor = g.getColor();

			Spot source, target;

			// Non-selected tracks.
			final int currentFrame = state.getCurrentTimepoint();
			final Set< Integer > filteredTrackIDs = model.getTrackModel().unsortedTrackIDs( true );

			if ( drawCellTriangles )
			{
				g.setStroke( halfSelectionStroke );
			}
			else
			{
				g.setStroke( normalStroke );
			}
			
			if ( trackDisplayMode == TrackDisplayMode.LOCAL )
				g.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER ) );

			// Determine bounds for limited view modes
			int minT = 0;
			int maxT = 0;
			switch ( trackDisplayMode )
			{
			case LOCAL:
			case SELECTION_ONLY:
				minT = currentFrame - trackDisplayDepth;
				maxT = currentFrame + trackDisplayDepth;
				break;
			case LOCAL_FORWARD:
				minT = currentFrame;
				maxT = currentFrame + trackDisplayDepth;
				break;
			case LOCAL_BACKWARD:
				minT = currentFrame - trackDisplayDepth;
				maxT = currentFrame;
				break;
			default:
				break;
			}

			double sourceFrame;
			float transparency;
			switch ( trackDisplayMode )
			{

			case FULL:
			{
				for ( final Integer trackID : filteredTrackIDs )
				{
					final Set< DefaultWeightedEdge > track = new HashSet<>( model.getTrackModel().trackEdges( trackID ) );
					for ( final DefaultWeightedEdge edge : track )
					{
						source = model.getTrackModel().getEdgeSource( edge );
						target = model.getTrackModel().getEdgeTarget( edge );
						g.setColor( trackColorGenerator.color( edge ) );
						drawEdge( g, source, target, transform, 1f, doLimitDrawingDepth, drawingDepth );
					}
				}

				break;
			}

			case SELECTION_ONLY:
			{

				// Sort edges by their track id.
				final HashMap< Integer, ArrayList< DefaultWeightedEdge > > sortedEdges = new HashMap<>();
				for ( final DefaultWeightedEdge edge : selectionModel.getEdgeSelection() )
				{
					final Integer trackID = model.getTrackModel().trackIDOf( edge );
					ArrayList< DefaultWeightedEdge > edges = sortedEdges.get( trackID );
					if ( null == edges )
					{
						edges = new ArrayList<>();
						sortedEdges.put( trackID, edges );
					}
					edges.add( edge );
				}

				for ( final Integer trackID : sortedEdges.keySet() )
				{
					for ( final DefaultWeightedEdge edge : sortedEdges.get( trackID ) )
					{
						source = model.getTrackModel().getEdgeSource( edge );
						target = model.getTrackModel().getEdgeTarget( edge );

						sourceFrame = source.getFeature( Spot.FRAME ).intValue();
						if ( sourceFrame < minT || sourceFrame >= maxT )
							continue;

						transparency = ( float ) ( 1 - Math.abs( sourceFrame - currentFrame ) / trackDisplayDepth );
						target = model.getTrackModel().getEdgeTarget( edge );
						g.setColor( trackColorGenerator.color( edge ) );
						drawEdge( g, source, target, transform, transparency, doLimitDrawingDepth, drawingDepth );
					}
				}
				break;
			}

			case LOCAL:
			case LOCAL_FORWARD:
			case LOCAL_BACKWARD:
			{

				g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

				for ( final int trackID : filteredTrackIDs )
				{
					final Set< DefaultWeightedEdge > track = model.getTrackModel().trackEdges( trackID );

					for ( final DefaultWeightedEdge edge : track )
					{
						source = model.getTrackModel().getEdgeSource( edge );
						sourceFrame = source.getFeature( Spot.FRAME ).intValue();
						target = model.getTrackModel().getEdgeTarget( edge );
						
						if ( sourceFrame < minT || sourceFrame >= maxT )
							continue;
						
						if ( sourceFrame == currentFrame && drawCellTriangles )
						{
							final double[] triangleCenter =  new double[ 3 ];
							final double[] localEnd =  new double[ 3 ];
							boolean forceDraw = !doLimitDrawingDepth;
							final double spotRadius = source.getFeature( Spot.RADIUS );
							Color color;
							
						
							//trangle center at source, and find this edge's endpoint to determine vector where triangle will point
							double[] globalCoords = new double[] { source.getFeature( Spot.POSITION_X ), source.getFeature( Spot.POSITION_Y ), source.getFeature( Spot.POSITION_Z ) };
							transform.apply( globalCoords, triangleCenter );
							globalCoords = new double[] { target.getFeature( Spot.POSITION_X ), target.getFeature( Spot.POSITION_Y ), target.getFeature( Spot.POSITION_Z ) };
							transform.apply( globalCoords, localEnd );
							
							//set color							
							if ( selectionModel.getSpotSelection().contains( source ) && trackDisplayMode != TrackDisplayMode.SELECTION_ONLY )
							{
								Stroke stroke;
								forceDraw = true; // Selection is drawn unconditionally.
								color = ds.getHighlightColor();
								stroke = selectionStroke;
								g.setStroke( stroke );
							}
							else
							{
								color = spotColorGenerator.color( source );
							}
						
							//now, draw triangle as established above
							if ( forceDraw || Math.abs( triangleCenter[2] ) <= drawingDepth )
							{
								g.setColor( color );
								g.setComposite( originalComposite );
								
								//final double dz2 = triangleCenter[ 2 ] * triangleCenter[ 2 ];
								final double rad = Math.pow(spotRadius * transformScale * radiusRatio,2);
								final double zv = triangleCenter[ 2 ];
								final double dz2 = zv * zv;	
								
								//determine rise/run for the local track
								//final double[] triangleVector = new double[] {localEnd[0] - triangleCenter[0],localEnd[1] - triangleCenter[1],localEnd[2] - triangleCenter[2]};
								final double[] triangleVector = new double[] {localEnd[0] - triangleCenter[0],localEnd[1] - triangleCenter[1],0f};
								
								//are we in view or not; if not, shrink radius considerably
								final double arad = Math.sqrt( rad - dz2 ) / 2; //should actually divide by sqrt(3) but 2 is easier
						
								/*
								if ( dz2 < rad )
								{
									g.drawOval( ( int ) ( triangleCenter[ 0 ] - arad ), ( int ) ( triangleCenter[ 1 ] - arad ), ( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );
								}
								else
								{
									g.fillOval( ( int ) triangleCenter[ 0 ] - 2, ( int ) triangleCenter[ 1 ] - 2, 4, 4 );
								}
								*/
									
								//normalize vector length to the desired radius
								final double vecNormalize = Math.sqrt(triangleVector[0]*triangleVector[0] + triangleVector[1]*triangleVector[1] + triangleVector[2]*triangleVector[2]) / arad;
								//final double vecNormalize = Math.sqrt(triangleVector[0]*triangleVector[0] + triangleVector[1]*triangleVector[1]) / arad;
								triangleVector[0] /= vecNormalize;
								triangleVector[1] /= vecNormalize;
								//triangleVector[2] /= vecNormalize; //Z coordinate actually doesn't need to get normalized
	
								//rotate the trajectory vector +120 and -120 in the Z axis, to produce the vectors emanating from triangleCenter and ending on the other two points of the triangle
								final int[] x = new int[] {(int)(triangleCenter[0]+(1.5*triangleVector[0])),(int)(triangleCenter[0]+triangleVector[0]*COSINE_120-triangleVector[1]*SINE_120),(int)(triangleCenter[0]+triangleVector[0]*COSINE_NEG120-triangleVector[1]*SINE_NEG120)};
								final int[] y = new int[] {(int)(triangleCenter[1]+(1.5*triangleVector[1])),(int)(triangleCenter[1]+triangleVector[0]*SINE_120+triangleVector[1]*COSINE_120),(int)(triangleCenter[1]+triangleVector[0]*SINE_NEG120+triangleVector[1]*COSINE_NEG120)};
								
								g.drawPolygon(x,y,3);
								
								//reset stroke for edge drawing, in case was bumped up earlier
								g.setStroke( halfSelectionStroke );
							}
						}
						
						transparency = ( float ) ( 1 - Math.abs( sourceFrame - currentFrame ) / trackDisplayDepth );

						target = model.getTrackModel().getEdgeTarget( edge );
						g.setColor( trackColorGenerator.color( edge ) );
						drawEdge( g, source, target, transform, transparency, doLimitDrawingDepth, drawingDepth );
					}
				}
				break;

			}

			}

			if ( trackDisplayMode != TrackDisplayMode.SELECTION_ONLY )
			{
				// Deal with highlighted edges first: brute and thick display
				g.setStroke( selectionStroke );
				g.setColor( ds.getHighlightColor() );
				for ( final DefaultWeightedEdge edge : selectionModel.getEdgeSelection() )
				{
					source = model.getTrackModel().getEdgeSource( edge );
					target = model.getTrackModel().getEdgeTarget( edge );
					drawEdge( g, source, target, transform, 1f, false, drawingDepth );
				}
			}

			// Restore graphic device original settings
			g.setComposite( originalComposite );
			g.setStroke( originalStroke );
			g.setColor( originalColor );

		}

	}

	protected void drawEdge( final Graphics2D g2d, final Spot source, final Spot target, final AffineTransform3D tr, final float transparency, final boolean limitDrawingDetph, final double drawingDepth )
	{

		// Find x & y & z in physical coordinates
		final double x0i = source.getFeature( Spot.POSITION_X );
		final double y0i = source.getFeature( Spot.POSITION_Y );
		final double z0i = source.getFeature( Spot.POSITION_Z );
		final double[] physicalPositionSource = new double[] { x0i, y0i, z0i };

		final double x1i = target.getFeature( Spot.POSITION_X );
		final double y1i = target.getFeature( Spot.POSITION_Y );
		final double z1i = target.getFeature( Spot.POSITION_Z );
		final double[] physicalPositionTarget = new double[] { x1i, y1i, z1i };

		// In pixel units
		final double[] pixelPositionSource = new double[ 3 ];
		tr.apply( physicalPositionSource, pixelPositionSource );
		final double[] pixelPositionTarget = new double[ 3 ];
		tr.apply( physicalPositionTarget, pixelPositionTarget );

		if ( limitDrawingDetph && Math.abs( pixelPositionSource[ 2 ] ) > drawingDepth && Math.abs( pixelPositionTarget[ 2 ] ) > drawingDepth )
			return;

		// Round
		final int x0 = ( int ) Math.round( pixelPositionSource[ 0 ] );
		final int y0 = ( int ) Math.round( pixelPositionSource[ 1 ] );
		final int x1 = ( int ) Math.round( pixelPositionTarget[ 0 ] );
		final int y1 = ( int ) Math.round( pixelPositionTarget[ 1 ] );

		g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, transparency ) );
		g2d.drawLine( x0, y0, x1, y1 );
	}

	/**
	 * Update data to show in the overlay.
	 *
	 * @param state
	 *            the state of the data.
	 */
	public void setViewerState( final ViewerState state )
	{
		this.state = state;
	}
}
