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
package fiji.plugin.mamut.gui;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.EDIT_SETTINGS_ICON;
import static fiji.plugin.trackmate.gui.Icons.SPOT_TABLE_ICON;
import static fiji.plugin.trackmate.gui.Icons.TRACK_SCHEME_ICON_16x16;
import static fiji.plugin.trackmate.gui.Icons.TRACK_TABLES_ICON;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.displaysettings.ConfigTrackMateDisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.UpdateListener;

public class MamutControlPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final ImageIcon MAMUT_ICON = new ImageIcon( MaMuT.class.getResource( "mammouth-16x16.png" ) );
	private static final ImageIcon SAVE_ICON = new ImageIcon( MaMuT.class.getResource( "page_save.png" ) );
	private static final Color BORDER_COLOR = new java.awt.Color( 192, 192, 192 );
	private static final String TRACK_TABLES_BUTTON_TOOLTIP = "<html>"
			+ "Export the features of all tracks, edges and all <br>"
			+ "spots belonging to a track to ImageJ tables."
			+ "</html>";
	private static final String SPOT_TABLE_BUTTON_TOOLTIP = "Export the features of all spots to ImageJ tables.";
	private static final String TRACKSCHEME_BUTTON_TOOLTIP = "<html>Launch a new instance of TrackScheme.</html>";


	/*
	 * CONSTRUCTORS
	 */

	public MamutControlPanel(
			final DisplaySettings ds,
			final FeatureDisplaySelector featureSelector,
			final String spaceUnits,
			final ActionListener launchMamutViewerAction,
			final ActionListener launchTrackSchemeAction,
			final ActionListener showTrackTablesAction,
			final ActionListener showSpotTableAction,
			final ActionListener saveAction )
	{
		this.setPreferredSize( new Dimension( 300, 521 ) );
		this.setSize( 300, 500 );

		final GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 1.0, 1.0 };
		layout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		setLayout( layout );

		/*
		 * Title
		 */

		final JLabel lblDisplayOptions = new JLabel();
		lblDisplayOptions.setText( "Display options" );
		lblDisplayOptions.setFont( BIG_FONT );
		lblDisplayOptions.setHorizontalAlignment( SwingConstants.LEFT );
		final GridBagConstraints gbcLabelDisplayOptions = new GridBagConstraints();
		gbcLabelDisplayOptions.gridwidth = 1;
		gbcLabelDisplayOptions.fill = GridBagConstraints.BOTH;
		gbcLabelDisplayOptions.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelDisplayOptions.gridx = 0;
		gbcLabelDisplayOptions.gridy = 0;
		add( lblDisplayOptions, gbcLabelDisplayOptions );

		/*
		 * Settings editor.
		 */

		final JFrame editor = ConfigTrackMateDisplaySettings.editor( ds,
				"Configure the display settings used in this current session.",
				"TrackMate display settings" );
		editor.setLocationRelativeTo( this.getParent() );
		editor.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );

		final JButton btnEditSettings = new JButton( "Edit settings", EDIT_SETTINGS_ICON );
		btnEditSettings.addActionListener( e -> editor.setVisible( !editor.isVisible() ) );

		final GridBagConstraints gbcBtnEditSettings = new GridBagConstraints();
		gbcBtnEditSettings.fill = GridBagConstraints.NONE;
		gbcBtnEditSettings.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnEditSettings.anchor = GridBagConstraints.EAST;
		gbcBtnEditSettings.gridx = 1;
		gbcBtnEditSettings.gridy = 0;
		add( btnEditSettings, gbcBtnEditSettings );

		/*
		 * Display spot checkbox.
		 */

		final JCheckBox chkboxDisplaySpots = new JCheckBox();
		chkboxDisplaySpots.setText( "Display spots" );
		chkboxDisplaySpots.setFont( FONT );
		final GridBagConstraints gbcCheckBoxDisplaySpots = new GridBagConstraints();
		gbcCheckBoxDisplaySpots.anchor = GridBagConstraints.NORTH;
		gbcCheckBoxDisplaySpots.fill = GridBagConstraints.HORIZONTAL;
		gbcCheckBoxDisplaySpots.insets = new Insets( 0, 5, 0, 5 );
		gbcCheckBoxDisplaySpots.gridx = 0;
		gbcCheckBoxDisplaySpots.gridy = 1;
		add( chkboxDisplaySpots, gbcCheckBoxDisplaySpots );

		/*
		 * Spot options panel.
		 */

		final JPanel panelSpotOptions = new JPanel();
		panelSpotOptions.setBorder( new LineBorder( BORDER_COLOR, 1, true ) );
		final GridBagConstraints gbcPanelSpotOptions = new GridBagConstraints();
		gbcPanelSpotOptions.gridwidth = 2;
		gbcPanelSpotOptions.insets = new Insets( 0, 5, 5, 5 );
		gbcPanelSpotOptions.fill = GridBagConstraints.BOTH;
		gbcPanelSpotOptions.gridx = 0;
		gbcPanelSpotOptions.gridy = 2;
		add( panelSpotOptions, gbcPanelSpotOptions );
		final GridBagLayout gblPanelSpotOptions = new GridBagLayout();
		gblPanelSpotOptions.columnWeights = new double[] { 0.0, 1.0 };
		gblPanelSpotOptions.rowWeights = new double[] { 0.0, 0.0 };
		panelSpotOptions.setLayout( gblPanelSpotOptions );

		final JLabel lblSpotRadius = new JLabel( "Spot display radius ratio:" );
		lblSpotRadius.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSpotRadius = new GridBagConstraints();
		gbcLblSpotRadius.anchor = GridBagConstraints.EAST;
		gbcLblSpotRadius.insets = new Insets( 5, 5, 0, 5 );
		gbcLblSpotRadius.gridx = 0;
		gbcLblSpotRadius.gridy = 0;
		panelSpotOptions.add( lblSpotRadius, gbcLblSpotRadius );

		final JFormattedTextField ftfSpotRadius = new JFormattedTextField();
		GuiUtils.selectAllOnFocus( ftfSpotRadius );
		ftfSpotRadius.setHorizontalAlignment( SwingConstants.CENTER );
		ftfSpotRadius.setFont( SMALL_FONT );
		ftfSpotRadius.setMinimumSize( new Dimension( 80, 20 ) );
		ftfSpotRadius.setColumns( 5 );
		final GridBagConstraints gbcFtfSpotRadius = new GridBagConstraints();
		gbcFtfSpotRadius.insets = new Insets( 5, 0, 0, 0 );
		gbcFtfSpotRadius.anchor = GridBagConstraints.WEST;
		gbcFtfSpotRadius.gridx = 1;
		gbcFtfSpotRadius.gridy = 0;
		panelSpotOptions.add( ftfSpotRadius, gbcFtfSpotRadius );

		final JLabel lblSpotName = new JLabel( "Display spot names:" );
		lblSpotName.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSpotName = new GridBagConstraints();
		gbcLblSpotName.anchor = GridBagConstraints.EAST;
		gbcLblSpotName.insets = new Insets( 0, 0, 0, 5 );
		gbcLblSpotName.gridx = 0;
		gbcLblSpotName.gridy = 1;
		panelSpotOptions.add( lblSpotName, gbcLblSpotName );

		final JCheckBox chkboxSpotNames = new JCheckBox();
		final GridBagConstraints gbcChkboxSpotNames = new GridBagConstraints();
		gbcChkboxSpotNames.anchor = GridBagConstraints.WEST;
		gbcChkboxSpotNames.gridx = 1;
		gbcChkboxSpotNames.gridy = 1;
		panelSpotOptions.add( chkboxSpotNames, gbcChkboxSpotNames );

		final JPanel selectorForSpots = featureSelector.createSelectorForSpots();
		final GridBagConstraints gbcCmbboxSpotColor = new GridBagConstraints();
		gbcCmbboxSpotColor.insets = new Insets( 0, 5, 5, 5 );
		gbcCmbboxSpotColor.fill = GridBagConstraints.BOTH;
		gbcCmbboxSpotColor.gridwidth = 2;
		gbcCmbboxSpotColor.gridx = 0;
		gbcCmbboxSpotColor.gridy = 3;
		panelSpotOptions.add( selectorForSpots, gbcCmbboxSpotColor );

		/*
		 * Tracks.
		 */

		final JCheckBox chkboxDisplayTracks = new JCheckBox();
		chkboxDisplayTracks.setText( "Display tracks" );
		chkboxDisplayTracks.setFont( FONT );
		final GridBagConstraints gbcCheckBoxDisplayTracks = new GridBagConstraints();
		gbcCheckBoxDisplayTracks.anchor = GridBagConstraints.NORTH;
		gbcCheckBoxDisplayTracks.fill = GridBagConstraints.HORIZONTAL;
		gbcCheckBoxDisplayTracks.insets = new Insets( 5, 5, 0, 5 );
		gbcCheckBoxDisplayTracks.gridx = 0;
		gbcCheckBoxDisplayTracks.gridy = 3;
		add( chkboxDisplayTracks, gbcCheckBoxDisplayTracks );

		final JComboBox< TrackDisplayMode > cmbboxTrackDisplayMode = new JComboBox<>( TrackDisplayMode.values() );
		final GridBagConstraints gbc_cmbboxTrackDisplayMode = new GridBagConstraints();
		gbc_cmbboxTrackDisplayMode.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbboxTrackDisplayMode.insets = new Insets( 5, 0, 0, 5 );
		gbc_cmbboxTrackDisplayMode.gridx = 1;
		gbc_cmbboxTrackDisplayMode.gridy = 3;
		add( cmbboxTrackDisplayMode, gbc_cmbboxTrackDisplayMode );
		cmbboxTrackDisplayMode.setFont( SMALL_FONT );
		cmbboxTrackDisplayMode.addActionListener( e -> ds.setTrackDisplayMode( ( TrackDisplayMode ) cmbboxTrackDisplayMode.getSelectedItem() ) );
		cmbboxTrackDisplayMode.setSelectedItem( ds.getTrackDisplayMode() );

		/*
		 * Tracks display options.
		 */

		final JPanel panelTrackOptions = new JPanel();
		panelTrackOptions.setBorder( new LineBorder( BORDER_COLOR, 1, true ) );
		final GridBagConstraints gbcPanelTrackOptions = new GridBagConstraints();
		gbcPanelTrackOptions.gridwidth = 2;
		gbcPanelTrackOptions.insets = new Insets( 0, 5, 5, 5 );
		gbcPanelTrackOptions.fill = GridBagConstraints.BOTH;
		gbcPanelTrackOptions.gridx = 0;
		gbcPanelTrackOptions.gridy = 4;
		add( panelTrackOptions, gbcPanelTrackOptions );
		final GridBagLayout gblPanelTrackOptions = new GridBagLayout();
		gblPanelTrackOptions.columnWeights = new double[] { 0.0, 0.0, 1.0 };
		gblPanelTrackOptions.rowWeights = new double[] { 0.0, 0.0 };
		panelTrackOptions.setLayout( gblPanelTrackOptions );

		final JLabel lblFadeTracks = new JLabel( "Fade tracks in time:" );
		lblFadeTracks.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblFadeTracks = new GridBagConstraints();
		gbcLblFadeTracks.anchor = GridBagConstraints.EAST;
		gbcLblFadeTracks.insets = new Insets( 0, 5, 0, 5 );
		gbcLblFadeTracks.gridx = 0;
		gbcLblFadeTracks.gridy = 0;
		panelTrackOptions.add( lblFadeTracks, gbcLblFadeTracks );

		final JCheckBox chkboxFadeTracks = new JCheckBox();
		final GridBagConstraints gbcChckbxFadeTracks = new GridBagConstraints();
		gbcChckbxFadeTracks.insets = new Insets( 0, 0, 0, 5 );
		gbcChckbxFadeTracks.anchor = GridBagConstraints.WEST;
		gbcChckbxFadeTracks.gridx = 1;
		gbcChckbxFadeTracks.gridy = 0;
		panelTrackOptions.add( chkboxFadeTracks, gbcChckbxFadeTracks );

		final JLabel lblFadeRange = new JLabel( "Fade range:" );
		lblFadeRange.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblFadeRange = new GridBagConstraints();
		gbcLblFadeRange.anchor = GridBagConstraints.EAST;
		gbcLblFadeRange.insets = new Insets( 0, 0, 0, 5 );
		gbcLblFadeRange.gridx = 0;
		gbcLblFadeRange.gridy = 1;
		panelTrackOptions.add( lblFadeRange, gbcLblFadeRange );

		final SpinnerNumberModel fadeTrackDepthModel = new SpinnerNumberModel( ds.getFadeTrackRange(), 1, 1000, 1 );
		final JSpinner spinnerFadeRange = new JSpinner( fadeTrackDepthModel );
		spinnerFadeRange.setFont( SMALL_FONT );
		final GridBagConstraints gbcSpinnerFadeRange = new GridBagConstraints();
		gbcSpinnerFadeRange.insets = new Insets( 0, 0, 0, 5 );
		gbcSpinnerFadeRange.anchor = GridBagConstraints.WEST;
		gbcSpinnerFadeRange.gridx = 1;
		gbcSpinnerFadeRange.gridy = 1;
		panelTrackOptions.add( spinnerFadeRange, gbcSpinnerFadeRange );

		final JLabel lblFadeRangeUnits = new JLabel( "time-points" );
		lblFadeRangeUnits.setFont( SMALL_FONT );
		final GridBagConstraints gbcFadeRangeUnit = new GridBagConstraints();
		gbcFadeRangeUnit.anchor = GridBagConstraints.WEST;
		gbcFadeRangeUnit.gridx = 2;
		gbcFadeRangeUnit.gridy = 1;
		panelTrackOptions.add( lblFadeRangeUnits, gbcFadeRangeUnit );

		final JPanel selectorForTracks = featureSelector.createSelectorForTracks();
		final GridBagConstraints gbcCmbboxTrackColor = new GridBagConstraints();
		gbcCmbboxTrackColor.insets = new Insets( 5, 5, 5, 5 );
		gbcCmbboxTrackColor.fill = GridBagConstraints.BOTH;
		gbcCmbboxTrackColor.gridwidth = 3;
		gbcCmbboxTrackColor.gridx = 0;
		gbcCmbboxTrackColor.gridy = 3;
		panelTrackOptions.add( selectorForTracks, gbcCmbboxTrackColor );

		/*
		 * Draw Z Depth
		 */

		final FlowLayout flowLayout = new FlowLayout( FlowLayout.LEFT, 5, 2 );
		final JPanel panelDrawingZDepth = new JPanel( flowLayout );
		panelDrawingZDepth.setBorder( new LineBorder( BORDER_COLOR, 1, true ) );
		final GridBagConstraints gbcPanelDrawingZDepth = new GridBagConstraints();
		gbcPanelDrawingZDepth.gridwidth = 2;
		gbcPanelDrawingZDepth.insets = new Insets( 0, 5, 5, 5 );
		gbcPanelDrawingZDepth.fill = GridBagConstraints.BOTH;
		gbcPanelDrawingZDepth.gridx = 0;
		gbcPanelDrawingZDepth.gridy = 5;
		add( panelDrawingZDepth, gbcPanelDrawingZDepth );

		final JCheckBox chkboxLimitZDepth = new JCheckBox( "Limit drawing Z depth" );
		chkboxLimitZDepth.setFont( SMALL_FONT );
		panelDrawingZDepth.add( chkboxLimitZDepth );

		final SpinnerNumberModel numberModelDrawingZDepth = new SpinnerNumberModel( ds.getZDrawingDepth(), 0.5, 5000., 0.5 );
		final JSpinner spinnerDrawingZDepth = new JSpinner( numberModelDrawingZDepth );
		spinnerDrawingZDepth.setFont( SMALL_FONT );
		panelDrawingZDepth.add( spinnerDrawingZDepth );

		final JLabel lblDrawingZDepthUnits = new JLabel( spaceUnits );
		lblDrawingZDepthUnits.setFont( SMALL_FONT );
		panelDrawingZDepth.add( lblDrawingZDepthUnits );

		/*
		 * Panel for view buttons.
		 */

		final JPanel panelButtons = new JPanel();

		// New Mamut viewer button
		final JButton btnMamutViewer = new JButton( "MaMuT viewer", MAMUT_ICON );
		btnMamutViewer.addActionListener( launchMamutViewerAction );
		panelButtons.add( btnMamutViewer );
		btnMamutViewer.setFont( FONT );

		// TrackScheme button.
		final JButton btnShowTrackScheme = new JButton( "TrackScheme", TRACK_SCHEME_ICON_16x16 );
		btnShowTrackScheme.setToolTipText( TRACKSCHEME_BUTTON_TOOLTIP );
		btnShowTrackScheme.addActionListener( launchTrackSchemeAction );
		panelButtons.add( btnShowTrackScheme );
		btnShowTrackScheme.setFont( FONT );

		// Track table.
		final JButton btnShowTrackTables = new JButton( "Track tables", TRACK_TABLES_ICON );
		btnShowTrackTables.addActionListener( showTrackTablesAction );
		btnShowTrackTables.setToolTipText( TRACK_TABLES_BUTTON_TOOLTIP );
		panelButtons.add( btnShowTrackTables );
		btnShowTrackTables.setFont( FONT );

		// Spot table.
		final JButton btnShowSpotTable = new JButton( "Spot table", SPOT_TABLE_ICON );
		btnShowSpotTable.addActionListener( showSpotTableAction );
		btnShowSpotTable.setToolTipText( SPOT_TABLE_BUTTON_TOOLTIP );
		panelButtons.add( btnShowSpotTable );
		btnShowSpotTable.setFont( FONT );

		// Save button
		final JButton btnSaveButton = new JButton( "Save", SAVE_ICON );
		btnSaveButton.addActionListener( saveAction );
		panelButtons.add( btnSaveButton );
		btnSaveButton.setFont( FONT );

		final GridBagConstraints gbcPanelButtons = new GridBagConstraints();
		gbcPanelButtons.gridwidth = 2;
		gbcPanelButtons.anchor = GridBagConstraints.NORTH;
		gbcPanelButtons.fill = GridBagConstraints.BOTH;
		gbcPanelButtons.gridx = 0;
		gbcPanelButtons.gridy = 7;
		add( panelButtons, gbcPanelButtons );

		/*
		 * Listeners & co.
		 */

		chkboxDisplaySpots.addActionListener( e -> setEnabled( panelSpotOptions, chkboxDisplaySpots.isSelected() ) );
		chkboxDisplayTracks.addActionListener( e -> {
			setEnabled( panelTrackOptions, chkboxDisplayTracks.isSelected() );
			cmbboxTrackDisplayMode.setEnabled( chkboxDisplayTracks.isSelected() );
		} );

		final ActionListener fadeTrackBtnEnable = e -> {
			final boolean shouldBeEnabled = chkboxDisplayTracks.isSelected()
					&& cmbboxTrackDisplayMode.getSelectedItem() != TrackDisplayMode.FULL;
			chkboxFadeTracks.setEnabled( shouldBeEnabled );
		};
		chkboxDisplayTracks.addActionListener( fadeTrackBtnEnable );
		cmbboxTrackDisplayMode.addActionListener( fadeTrackBtnEnable );

		final ActionListener fadeTrackRangeEnable = e -> {
			final boolean shouldBeEnabled = chkboxDisplayTracks
					.isSelected()
					&& cmbboxTrackDisplayMode.getSelectedItem() != TrackDisplayMode.FULL
					&& chkboxFadeTracks.isSelected();
			spinnerFadeRange.setEnabled( shouldBeEnabled );
		};
		cmbboxTrackDisplayMode.addActionListener( fadeTrackRangeEnable );
		chkboxDisplayTracks.addActionListener( fadeTrackRangeEnable );
		chkboxFadeTracks.addActionListener( fadeTrackRangeEnable );

		chkboxLimitZDepth.addActionListener( e -> spinnerDrawingZDepth.setEnabled( chkboxLimitZDepth.isSelected() ) );

		chkboxDisplaySpots.addActionListener( e -> ds.setSpotVisible( chkboxDisplaySpots.isSelected() ) );
		ftfSpotRadius.addPropertyChangeListener( "value", e -> ds.setSpotDisplayRadius( ( ( Number ) ftfSpotRadius.getValue() ).doubleValue() ) );
		chkboxSpotNames.addActionListener( e -> ds.setSpotShowName( chkboxSpotNames.isSelected() ) );
		chkboxDisplayTracks.addActionListener( e -> ds.setTrackVisible( chkboxDisplayTracks.isSelected() ) );
		chkboxFadeTracks.addActionListener( e -> ds.setFadeTracks( chkboxFadeTracks.isSelected() ) );
		fadeTrackDepthModel.addChangeListener( e -> ds.setFadeTrackRange( fadeTrackDepthModel.getNumber().intValue() ) );
		chkboxLimitZDepth.addActionListener( e -> ds.setZDrawingDepthLimited( chkboxLimitZDepth.isSelected() ) );
		numberModelDrawingZDepth.addChangeListener( e -> ds.setZDrawingDepth( numberModelDrawingZDepth.getNumber().doubleValue() ) );

		/*
		 * Set current values.
		 */

		final UpdateListener l = () -> {
			chkboxDisplaySpots.setSelected( ds.isSpotVisible() );
			chkboxSpotNames.setSelected( ds.isSpotShowName() );
			chkboxDisplayTracks.setSelected( ds.isTrackVisible() );
			chkboxFadeTracks.setSelected( ds.isFadeTracks() );
			chkboxLimitZDepth.setSelected( ds.isZDrawingDepthLimited() );
			ftfSpotRadius.setValue( Double.valueOf( ds.getSpotDisplayRadius() ) );
			fadeTrackDepthModel.setValue( Integer.valueOf( ds.getFadeTrackRange() ) );
			numberModelDrawingZDepth.setValue( Double.valueOf( ds.getZDrawingDepth() ) );
			cmbboxTrackDisplayMode.setSelectedItem( ds.getTrackDisplayMode() );

			setEnabled( panelSpotOptions, chkboxDisplaySpots.isSelected() );
			setEnabled( panelTrackOptions, chkboxDisplayTracks.isSelected() );
		};
		l.displaySettingsChanged();
		ds.listeners().add( l );
		spinnerDrawingZDepth.setEnabled( chkboxLimitZDepth.isSelected() );
		fadeTrackBtnEnable.actionPerformed( null );
	}

	private static final void setEnabled( final Container container, final boolean enabled )
	{
		for ( final Component component : container.getComponents() )
		{
			if ( component instanceof JSpinner || component instanceof JCheckBox )
				continue; // Treat them elsewhere.
			component.setEnabled( enabled );
			if ( component instanceof Container )
				setEnabled( ( Container ) component, enabled );
		}
	}
}
