/**
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.da;

import static org.hibernate.criterion.Restrictions.between;
import static org.hibernate.criterion.Restrictions.isNull;
import static org.hibernate.criterion.Restrictions.like;
import static org.hibernate.criterion.Restrictions.or;
import static org.n52.series.db.da.beans.DataModelUtil.isEntitySupported;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.Interval;
import org.n52.io.crs.BoundingBox;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

public abstract class AbstractDbQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDbQuery.class);

    protected static final String COLUMN_KEY = "pkid";

    private static final String COLUMN_LOCALE = "locale";

    private static final String COLUMN_TIMESTAMP = "timestamp";

    private IoParameters parameters = IoParameters.createDefaults();

    private String sridAuthorityCode = "EPSG:4326"; // default

    protected AbstractDbQuery(IoParameters parameters) {
        if (parameters != null) {
            this.parameters = parameters;
        }
    }
    
    public abstract DetachedCriteria createDetachedFilterCriteria(String propertyName);

    public void setDatabaseAuthorityCode(String code) {
        this.sridAuthorityCode = code;
    }

    public String getLocale() {
        return parameters.getLocale();
    }

    public Interval getTimespan() {
        return parameters.getTimespan().toInterval();
    }

    public BoundingBox getSpatialFilter() {
        return parameters.getSpatialFilter();
    }

    public boolean checkTranslationForLocale(Criteria criteria) {
        return criteria.add(Restrictions.like(COLUMN_LOCALE, getCountryCode())).list().size() != 0;
    }

    public Criteria addLocaleTo(Criteria criteria, Class< ? > clazz) {
        if (getLocale() != null && isEntitySupported(clazz, criteria)) {
            criteria = criteria.createCriteria("translations", JoinType.LEFT_OUTER_JOIN)
                    .add(or(like(COLUMN_LOCALE, getCountryCode()),
                            isNull(COLUMN_LOCALE)));
        }
        return criteria;
    }

    private String getCountryCode() {
        return getLocale().split("_")[0];
    }

    public Criteria addPagingTo(Criteria criteria) {
        if (parameters.getLimit() > 0) {
            criteria.setMaxResults(parameters.getLimit());
        }
        if (parameters.getOffset() > 0) {
            criteria.setFirstResult(parameters.getOffset());
        }
        return criteria;
    }

    public Criteria addTimespanTo(Criteria criteria) {
        if (parameters.getTimespan() != null) {
            Interval interval = parameters.getTimespan().toInterval();
            Date start = interval.getStart().toDate();
            Date end = interval.getEnd().toDate();
            criteria.add(between(COLUMN_TIMESTAMP, start, end));
        }
        return criteria;
    }

    /**
     * @param id
     *        the id string to parse.
     * @return the long value of given string or {@link Long#MIN_VALUE} if string could not be parsed to type
     *         long.
     */
    protected Long parseToId(String id) {
        try {
            return Long.parseLong(id);
        }
        catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }

    public Criteria addSpatialFilterTo(Criteria criteria, AbstractDbQuery parameters) {
        BoundingBox spatialFilter = parameters.getSpatialFilter();
        if (spatialFilter != null) {
            try {
                CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();
                int databaseSrid = crsUtils.getSrsIdFrom(sridAuthorityCode);
                Point ll = (Point) crsUtils.transformInnerToOuter(spatialFilter.getLowerLeft(), sridAuthorityCode);
                Point ur = (Point) crsUtils.transformInnerToOuter(spatialFilter.getUpperRight(), sridAuthorityCode);
                Envelope envelope = new Envelope(ll.getCoordinate(), ur.getCoordinate());
                criteria.add(SpatialRestrictions.filter("geom", envelope, databaseSrid));
            }
            catch (FactoryException e) {
                LOGGER.error("Could not create transformation facilities.", e);
            }
            catch (TransformException e) {
                LOGGER.error("Could not perform transformation.", e);
            }
        }
        return criteria;
    }
    
    public IoParameters getParameters() {
    	return parameters;
    }
    
}
