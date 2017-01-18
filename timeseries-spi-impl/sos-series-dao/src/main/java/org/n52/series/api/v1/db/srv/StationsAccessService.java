/**
 * Copyright (C) 2013-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.series.api.v1.db.srv;

import java.util.ArrayList;
import java.util.List;

import org.n52.io.IoParameters;
import org.n52.io.v1.data.StationOutput;
import org.n52.series.api.v1.db.da.DataAccessException;
import org.n52.series.api.v1.db.da.DbQuery;
import org.n52.series.api.v1.db.da.StationRepository;
import org.n52.web.InternalServerException;
import org.n52.sensorweb.v1.spi.ParameterService;

public class StationsAccessService extends ServiceInfoAccess implements ParameterService<StationOutput> {

    public StationsAccessService(String dbSrid) {
        if (dbSrid != null) {
            StationRepository repository = createStationRepository();
            repository.setDatabaseSrid(dbSrid);
        }
    }

    @Override
    public StationOutput[] getExpandedParameters(IoParameters query) {
        try {
            DbQuery dbQuery = DbQuery.createFrom(query);
            StationRepository repository = createStationRepository();
            List<StationOutput> results = repository.getAllExpanded(dbQuery);
            return results.toArray(new StationOutput[0]);
        }
        catch (DataAccessException e) {
            throw new InternalServerException("Could not get station data.");
        }
    }

    @Override
    public StationOutput[] getCondensedParameters(IoParameters query) {
        try {
            DbQuery dbQuery = DbQuery.createFrom(query);
            StationRepository repository = createStationRepository();
            List<StationOutput> results = repository.getAllCondensed(dbQuery);
            return results.toArray(new StationOutput[0]);
        }
        catch (DataAccessException e) {
            throw new InternalServerException("Could not get station data.");
        }
    }

    @Override
    public StationOutput[] getParameters(String[] stationsIds) {
        return getParameters(stationsIds, IoParameters.createDefaults());
    }

    @Override
    public StationOutput[] getParameters(String[] stationIds, IoParameters query) {
        try {
            DbQuery dbQuery = DbQuery.createFrom(query);
            StationRepository repository = createStationRepository();
            List<StationOutput> results = new ArrayList<StationOutput>();
            for (String stationId : stationIds) {
                results.add(repository.getInstance(stationId, dbQuery));
            }
            return results.toArray(new StationOutput[0]);
        }
        catch (DataAccessException e) {
            throw new InternalServerException("Could not get station data.");
        }
    }

    @Override
    public StationOutput getParameter(String stationsId) {
        return getParameter(stationsId, IoParameters.createDefaults());
    }

    @Override
    public StationOutput getParameter(String stationId, IoParameters query) {
        try {
            DbQuery dbQuery = DbQuery.createFrom(query);
            StationRepository repository = createStationRepository();
            return repository.getInstance(stationId, dbQuery);
        }
        catch (DataAccessException e) {
            throw new InternalServerException("Could not get station data.");
        }
    }

    private StationRepository createStationRepository() {
        return new StationRepository(getServiceInfo());
    }

}
