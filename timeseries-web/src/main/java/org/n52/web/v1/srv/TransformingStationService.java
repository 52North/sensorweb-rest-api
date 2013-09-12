/**
 * ﻿Copyright (C) 2013
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */

package org.n52.web.v1.srv;

import org.n52.io.IoParameters;
import org.n52.io.v1.data.StationOutput;

/**
 * Composes a {@link ParameterService} for {@link StationOutput}s to transform geometries to requested spatial
 * reference system.
 */
public class TransformingStationService extends TransformationService implements ParameterService<StationOutput> {

    private ParameterService<StationOutput> composedService;

    public TransformingStationService(ParameterService<StationOutput> toCompose) {
        this.composedService = toCompose;
    }

    @Override
    public StationOutput[] getExpandedParameters(IoParameters query) {
        StationOutput[] stations = composedService.getExpandedParameters(query);
        return transformStations(query, stations);
    }

    @Override
    public StationOutput[] getCondensedParameters(IoParameters query) {
        StationOutput[] stations = composedService.getCondensedParameters(query);
        return transformStations(query, stations);
    }

    @Override
    public StationOutput[] getParameters(String[] items) {
        StationOutput[] stations = composedService.getParameters(items);
        return transformStations(IoParameters.createDefaults(), stations);
    }

    @Override
    public StationOutput[] getParameters(String[] items, IoParameters query) {
        StationOutput[] stations = composedService.getParameters(items, query);
        return transformStations(query, stations);
    }

    @Override
    public StationOutput getParameter(String item) {
        StationOutput station = composedService.getParameter(item);
        transformInline(station, IoParameters.createDefaults());
        return station;
    }

    @Override
    public StationOutput getParameter(String item, IoParameters query) {
        StationOutput station = composedService.getParameter(item, query);
        transformInline(station, query);
        return station;
    }
    
}
