/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2021 MaMuT development team.
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
package fiji.plugin.mamut.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.action.CloseGapsByLinearInterpolationAction;
import fiji.plugin.trackmate.action.TrackMateAction;

@Plugin( type = MamutActionFactory.class )
public class MamutCloseGapsFactory implements MamutActionFactory
{

	@Override
	public String getInfoText()
	{
		return CloseGapsByLinearInterpolationAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return CloseGapsByLinearInterpolationAction.ICON;
	}

	@Override
	public String getKey()
	{
		return CloseGapsByLinearInterpolationAction.KEY;
	}

	@Override
	public String getName()
	{
		return CloseGapsByLinearInterpolationAction.NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		return new CloseGapsByLinearInterpolationAction();
	}
}
