/*
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.series.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.dataset.SeriesParameters;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.MeasurementDatasetEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.ServiceInfo;
import org.n52.series.db.da.ServiceRepository;
import org.n52.series.db.dao.DbQuery;
import org.n52.web.ctrl.UrlHelper;
import org.n52.web.exception.BadRequestException;
import org.n52.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SessionAwareRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionAwareRepository.class);

    private final CRSUtils crsUtils = CRSUtils.createEpsgStrictAxisOrder();

    private String databaseSrid; // if null, database is expected to have srs set properly

    @Autowired
    private HibernateSessionStore sessionStore;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceInfo serviceInfo;

    protected UrlHelper urHelper = new UrlHelper();

    protected ServiceOutput getServiceOutput() throws DataAccessException {
        List<ServiceOutput> all = serviceRepository.getAllCondensed(null);
        return all.get(0); // only this service available
    }

    protected DbQuery getDbQuery(IoParameters parameters) {
        return DbQuery.createFrom(parameters);
    }

    public HibernateSessionStore getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(HibernateSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public ServiceRepository getServiceRepository() {
        return serviceRepository;
    }

    public void setServiceRepository(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public void setServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    protected CRSUtils getCrsUtils() {
        return crsUtils;
    }

    protected String getDatabaseSrid() {
        return databaseSrid;
    }

    protected Long parseId(String id) throws BadRequestException {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            LOGGER.debug("Unable to parse {} to Long.", e);
            throw new ResourceNotFoundException("Resource with id '" + id + "' could not be found.");
        }
    }

    public void returnSession(Session session) {
        sessionStore.returnSession(session);
    }

    public Session getSession() {
        try {
            return sessionStore.getSession();
        } catch (Throwable e) {
            throw new IllegalStateException("Could not get hibernate session.", e);
        }
    }

    public void setDatabaseSrid(String databaseSrid) {
        this.databaseSrid = databaseSrid;
    }

    protected Map<String, SeriesParameters> createTimeseriesList(List<MeasurementDatasetEntity> series, DbQuery parameters) throws DataAccessException {
        Map<String, SeriesParameters> timeseriesOutputs = new HashMap<>();
        for (MeasurementDatasetEntity timeseries : series) {
            if (!timeseries.getProcedure().isReference()) {
                String timeseriesId = timeseries.getPkid().toString();
                timeseriesOutputs.put(timeseriesId, createTimeseriesOutput(timeseries, parameters));
            }
        }
        return timeseriesOutputs;
    }

    protected SeriesParameters createTimeseriesOutput(MeasurementDatasetEntity timeseries, DbQuery parameters) throws DataAccessException {
        SeriesParameters timeseriesOutput = new SeriesParameters();
        timeseriesOutput.setService(getCondensedService(parameters));
        timeseriesOutput.setOffering(getCondensedOffering(timeseries.getProcedure(), parameters));
        timeseriesOutput.setProcedure(getCondensedProcedure(timeseries.getProcedure(), parameters));
        timeseriesOutput.setPhenomenon(getCondensedPhenomenon(timeseries.getPhenomenon(), parameters));
        timeseriesOutput.setFeature(getCondensedFeature(timeseries.getFeature(), parameters));
        timeseriesOutput.setCategory(getCondensedCategory(timeseries.getCategory(), parameters));
        return timeseriesOutput;
    }

    protected SeriesParameters createSeriesParameters(DatasetEntity series, DbQuery parameters) throws DataAccessException {
        SeriesParameters seriesParameter = new SeriesParameters();
        seriesParameter.setService(getCondensedExtendedService(parameters));
        seriesParameter.setOffering(getCondensedExtendedOffering(series.getProcedure(), parameters));
        seriesParameter.setProcedure(getCondensedExtendedProcedure(series.getProcedure(), parameters));
        seriesParameter.setPhenomenon(getCondensedExtendedPhenomenon(series.getPhenomenon(), parameters));
        seriesParameter.setFeature(getCondensedExtendedFeature(series.getFeature(), parameters));
        seriesParameter.setCategory(getCondensedExtendedCategory(series.getCategory(), parameters));
        seriesParameter.setPlatform(getCondensedPlatform(series.getPlatform(), parameters));
        return seriesParameter;
    }

    protected ServiceOutput getCondensedService(DbQuery parameters ) throws DataAccessException {
        String serviceId = serviceRepository.getServiceId();
        ServiceOutput instance = serviceRepository.getCondensedInstance(serviceId, parameters);
        ServiceOutput serviceOutput = new ServiceOutput();
        serviceOutput.setLabel(instance.getLabel());
        serviceOutput.setId(instance.getId());
        return serviceOutput;
    }

    private ServiceOutput getCondensedExtendedService(DbQuery parameters) {
        String serviceId = serviceRepository.getServiceId();
        ServiceOutput instance = serviceRepository.getCondensedInstance(serviceId, parameters);
        ServiceOutput serviceOutput = new ServiceOutput();
        serviceOutput.setLabel(instance.getLabel());
        serviceOutput.setId(instance.getId());
        serviceOutput.setHref(urHelper.getServicesHrefBaseUrl(parameters.getHrefBase()) + "/" + instance.getId());
        return serviceOutput;
    }

    protected ParameterOutput getCondensedPhenomenon(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new PhenomenonOutput(), entity, parameters);
    }

    protected ParameterOutput getCondensedExtendedPhenomenon(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new PhenomenonOutput(), entity, parameters, urHelper.getPhenomenaHrefBaseUrl(parameters.getHrefBase()));
    }

    protected ParameterOutput getCondensedOffering(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new OfferingOutput(), entity, parameters);
    }

    protected ParameterOutput getCondensedExtendedOffering(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new OfferingOutput(), entity, parameters, urHelper.getOfferingsHrefBaseUrl(parameters.getHrefBase()));
    }

    private ParameterOutput createCondensed(ParameterOutput outputvalue, DescribableEntity entity, DbQuery parameters) {
        outputvalue.setLabel(entity.getLabelFrom(parameters.getLocale()));
        outputvalue.setId(Long.toString(entity.getPkid()));
        return outputvalue;
    }

    private ParameterOutput createCondensed(ParameterOutput outputvalue, DescribableEntity entity, DbQuery parameters, String hrefBase) {
        createCondensed(outputvalue, entity, parameters);
        outputvalue.setHref(hrefBase + "/" + outputvalue.getId());
        return outputvalue;
    }

    protected ParameterOutput getCondensedProcedure(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new ProcedureOutput(), entity, parameters);
    }

    protected ParameterOutput getCondensedExtendedProcedure(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new ProcedureOutput(), entity, parameters, urHelper.getProceduresHrefBaseUrl(parameters.getHrefBase()));
    }

    protected ParameterOutput getCondensedFeature(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new FeatureOutput(), entity, parameters);
    }

    protected ParameterOutput getCondensedExtendedFeature(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new FeatureOutput(), entity, parameters, urHelper.getFeaturesHrefBaseUrl(parameters.getHrefBase()));
    }

    protected ParameterOutput getCondensedCategory(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new CategoryOutput(), entity, parameters);
    }

    protected ParameterOutput getCondensedExtendedCategory(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new CategoryOutput(), entity, parameters, urHelper.getCategoriesHrefBaseUrl(parameters.getHrefBase()));
    }

    protected ParameterOutput getCondensedPlatform(PlatformEntity entity, DbQuery parameters) {
        return createCondensed(new PlatformOutput(entity.getPlatformType()), entity, parameters, urHelper.getPlatformsHrefBaseUrl(parameters.getHrefBase()));
    }


}
