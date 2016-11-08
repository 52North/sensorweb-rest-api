/**
 * Copyright (C) 2012-2016 52°North Initiative for Geospatial Open Source
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
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.connector;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db_custom.beans.CountDatasetTEntity;
import org.n52.series.db_custom.beans.DatasetTEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db_custom.beans.MeasurementDatasetTEntity;
import org.n52.series.db_custom.beans.TextDatasetTEntity;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.util.JTSHelper;

public class EntityBuilder {

    public static ServiceEntity createService(String name, String description, String url, String version) {
        ServiceEntity service = new ServiceEntity();
        service.setServiceId(name);
        service.setServiceDescription(description);
        service.setVersion(version);
        service.setType("SOS");
        service.setUrl(url);
        return service;
    }

    public static ProcedureEntity createProcedure(String name, boolean insitu, boolean mobile, ServiceEntity service) {
        ProcedureEntity procedure = new ProcedureEntity();
        procedure.setName(name);
        procedure.setDomainId(name);
        procedure.setInsitu(insitu);
        procedure.setMobile(mobile);
        procedure.setService(service);
        return procedure;
    }

    public static CategoryEntity createCategory(String name, ServiceEntity service) {
        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        category.setDomainId(name);
        category.setService(service);
        return category;
    }

    public static FeatureEntity createFeature(String name, GeometryEntity geometry, ServiceEntity service) throws OwsExceptionReport {
        FeatureEntity feature = new FeatureEntity();
        feature.setName(name);
        feature.setDomainId(name);
        feature.setGeometryEntity(geometry);
        feature.setService(service);
        return feature;
    }

    public static GeometryEntity createGeometry(double latitude, double longitude) {
        GeometryEntity geometry = new GeometryEntity();
        try {
            geometry.setGeometry(JTSHelper.createGeometryFromWKT("POINT (" + longitude + " " + latitude + ")", 4326));
        } catch (OwsExceptionReport ex) {
            Logger.getLogger(EntityBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        return geometry;
    }

    public static PhenomenonEntity createPhenomenon(String name, ServiceEntity service) {
        PhenomenonEntity phenomenon = new PhenomenonEntity();
        phenomenon.setName(name);
        phenomenon.setDomainId(name);
        phenomenon.setService(service);
        return phenomenon;
    }

    public static UnitEntity createUnit(String unit, ServiceEntity service) {
        UnitEntity entity = new UnitEntity();
        entity.setName(unit);
        entity.setService(service);
        return entity;
    }

    public static MeasurementDatasetTEntity createMeasurementDataset(ProcedureEntity procedure, CategoryEntity category, FeatureEntity feature, PhenomenonEntity phenomenon, UnitEntity unit, ServiceEntity service) {
        MeasurementDatasetTEntity measurementDataset = new MeasurementDatasetTEntity();
        updateDataset(measurementDataset, procedure, category, feature, phenomenon, service);
        measurementDataset.setUnit(unit);
        measurementDataset.setFirstValueAt(new Date());
        measurementDataset.setLastValueAt(new Date());
        return measurementDataset;
    }

    public static TextDatasetTEntity createTextDataset(ProcedureEntity procedure, CategoryEntity category, FeatureEntity feature, PhenomenonEntity phenomenon, ServiceEntity service) {
        TextDatasetTEntity textDataset = new TextDatasetTEntity();
        updateDataset(textDataset, procedure, category, feature, phenomenon, service);
        return textDataset;
    }

    public static CountDatasetTEntity createCountDataset(ProcedureEntity procedure, CategoryEntity category, FeatureEntity feature, PhenomenonEntity phenomenon, ServiceEntity service) {
        CountDatasetTEntity countDataset = new CountDatasetTEntity();
        updateDataset(countDataset, procedure, category, feature, phenomenon, service);
        return countDataset;
    }

    private static void updateDataset(DatasetTEntity dataset, ProcedureEntity procedure, CategoryEntity category, FeatureEntity feature, PhenomenonEntity phenomenon, ServiceEntity service) {
        dataset.setProcedure(procedure);
        dataset.setCategory(category);
        dataset.setFeature(feature);
        dataset.setPhenomenon(phenomenon);
        dataset.setPublished(Boolean.TRUE);
        dataset.setDeleted(Boolean.FALSE);
        dataset.setService(service);
    }

}