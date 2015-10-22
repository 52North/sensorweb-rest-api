/**
 * Copyright (C) 2013-2015 52°North Initiative for Geospatial Open Source
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
package org.n52.io;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.geotools.util.MapEntry;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.n52.io.crs.BoundingBox;
import org.n52.io.crs.CRSUtils;
import static org.n52.io.crs.CRSUtils.DEFAULT_CRS;
import static org.n52.io.crs.CRSUtils.createEpsgForcedXYAxisOrder;
import static org.n52.io.crs.CRSUtils.createEpsgStrictAxisOrder;
import org.n52.io.geojson.GeojsonPoint;
import org.n52.io.img.ChartDimension;
import org.n52.io.style.LineStyle;
import org.n52.io.style.Style;
import org.n52.io.v1.data.BBox;
import org.n52.io.v1.data.DesignedParameterSet;
import org.n52.io.v1.data.ParameterSet;
import org.n52.io.v1.data.StyleProperties;
import org.n52.io.v1.data.UndesignedParameterSet;
import org.n52.io.v1.data.Vicinity;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;

public class IoParameters {

    private final static Logger LOGGER = LoggerFactory.getLogger(IoParameters.class);

    private final static String DEFAULT_CONFIG_FILE = "/config-general.json";

    // XXX refactor ParameterSet, DesignedParameterSet, UndesingedParameterSet and QueryMap

    /**
     * How detailed the output shall be.
     */
    static final String EXPANDED = "expanded";

    /**
     * The default expansion of collection items.
     *
     * @see #EXPANDED
     */
    private static final boolean DEFAULT_EXPANDED = false;

    /**
     * If latest values shall be requested in a bulk timeseries request.
     */
    static final String FORCE_LATEST_VALUE = "force_latest_values";

    /**
     * The default behaviour if latest value requests shall be invoked during a timeseries collection request.
     */
    private static final boolean DEFAULT_FORCE_LATEST_VALUE = false;

    /**
     * If status intervals section is requested.
     */
    static final String STATUS_INTERVALS = "status_intervals";

    /**
     * The default behaviour for status intervals.
     */
    private static final boolean DEFAULT_STATUS_INTERVALS = false;

    /**
     * If rendering hints are requested for a timeseries
     */
    static final String RENDERING_HINTS = "rendering_hints";

    /**
     * The default behaviour for rendering hints.
     */
    private static final boolean DEFAULT_RENDERING_HINTS = false;

    /**
     * Determines the index of the first member of the response page (a.k.a. page offset).
     */
    static final String OFFSET = "offset";

    /**
     * The default page offset.
     *
     * @see #OFFSET
     */
    private static final int DEFAULT_OFFSET = -1;

    /**
     * Determines the limit of the page to be returned.
     */
    static final String LIMIT = "limit";

    /**
     * The default page size limit.
     *
     * @see #LIMIT
     */
    private static final int DEFAULT_LIMIT = -1;

    /**
     * Determines the locale the output shall have.
     */
    static final String LOCALE = "locale";

    /**
     * The default locale.
     *
     * @see #LOCALE
     */
    private static final String DEFAULT_LOCALE = "en";

    /**
     * Determines the timespan parameter
     */
    static final String TIMESPAN = "timespan";

    /**
     * Parameter to specify the timeseries data with a result time
     */
    static final String RESULTTIME = "resultTime";

    /**
     * The width in px of the image to be rendered.
     */
    static final String WIDTH = "width";

    /**
     * The default width of the chart image to render.
     */
    private static final int DEFAULT_WIDTH = 800;

    /**
     * The height in px of the image to be rendered.
     */
    static final String HEIGHT = "height";

    /**
     * The default height of the chart image to render.
     */
    private static final int DEFAULT_HEIGHT = 500;

    /**
     * If a chart shall be rendered with a background grid.
     */
    static final String GRID = "grid";

    /**
     * Defaults to a background grid in a rendered chart.
     */
    private static final boolean DEFAULT_GRID = true;

    /**
     * If a legend shall be drawn on the chart.
     */
    static final String LEGEND = "legend";

    /**
     * Defaults to a not drawn legend.
     */
    private static final boolean DEFAULT_LEGEND = false;

    /**
     * If a rendered chart shall be written as base64 encoded string.
     */
    static final String BASE_64 = "base64";

    /**
     * Defaults to binary output.
     */
    private static final boolean DEFAULT_BASE_64 = false;

    /**
     * Determines the generalize flag.
     */
    static final String GENERALIZE = "generalize";

    /**
     * The default (no generalization) behaviour.
     */
    private static final boolean DEFAULT_GENERALIZE = false;

    /**
     * Determines how raw data shall be formatted.
     */
    static final String FORMAT = "format";

    /**
     * The default format for raw data output.
     */
    private static final String DEFAULT_FORMAT = "tvp";

    /**
     * Determines the style parameter
     */
    static final String STYLE = "style";

    /**
     * Determines the service filter
     */
    static final String SERVICE = "service";

    /**
     * Determines the feature filter
     */
    static final String FEATURE = "feature";

    /**
     * Determines the service filter
     */
    static final String OFFERING = "offering";

    /**
     * Determines the procedure filter
     */
    static final String PROCEDURE = "procedure";

    /**
     * Determines the phenomenon filter
     */
    static final String PHENOMENON = "phenomenon";

    /**
     * Determines the station filter
     */
    static final String STATION = "station";

    /**
     * Determines the category filter
     */
    static final String CATEGORY = "category";

    /**
     * Determines the reference system to be used for input/output coordinates.
     */
    static final String CRS = "crs";

    /**
     * Determines if CRS axes order shall always be XY, i.e. lon/lat.
     */
    static final String FORCE_XY = "forceXY";

    /**
     * Default axes order respects EPSG axes ordering.
     */
    private static final boolean DEFAULT_FORCE_XY = false;

    /**
     * Determines the within filter
     */
    static final String NEAR = "near";

    /**
     * Determines the bbox filter
     */
    static final String BBOX = "bbox";

    private Map<String, String> query;

    /**
     * Use static constructor {@link #createFromQuery(MultiValueMap)}.
     *
     * @param queryParameters
     *        containing query parameters. If <code>null</code>, all parameters are returned with default
     *        values.
     */
    protected IoParameters(Map<String, String> queryParameters) {
        query = readDefaultConfig();
        if (queryParameters != null) {
            // override defaults
            query.putAll(queryParameters);
        }
    }

    private Map<String, String> readDefaultConfig() {
        InputStream taskConfig = getClass().getResourceAsStream(DEFAULT_CONFIG_FILE);
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(taskConfig, HashMap.class);
        }
        catch (IOException e) {
            LOGGER.error("Could not load {}. Using empty config.", DEFAULT_CONFIG_FILE, e);
            return new HashMap<String, String>();
        }
        finally {
            if (taskConfig != null) {
                try {
                    taskConfig.close();
                }
                catch (IOException e) {
                    LOGGER.debug("Stream already closed.");
                }
            }
        }
    }

    /**
     * @return the value of {@value #OFFSET} parameter. If not present, the default {@value #DEFAULT_OFFSET}
     *         is returned.
     * @throws IoParseException
     *         if parameter could not be parsed.
     */
    public int getOffset() {
        if ( !query.containsKey(OFFSET)) {
            return DEFAULT_OFFSET;
        }
        return parseInteger(OFFSET);
    }

    /**
     * @return the value of {@value #LIMIT} parameter. If not present, the default {@value #DEFAULT_LIMIT} is
     *         returned.
     * @throws IoParseException
     *         if parameter could not be parsed.
     */
    public int getLimit() {
        if ( !query.containsKey(LIMIT)) {
            return DEFAULT_LIMIT;
        }
        return parseInteger(LIMIT);
    }

    /**
     * @return the chart dimensions. If {@value #WIDTH} and {@value #HEIGHT} parameters are missing the
     *         defaults are used: <code>width=</code>{@value #DEFAULT_WIDTH}, <code>height=</code>
     *         {@value #DEFAULT_HEIGHT}
     * @throws IoParseException
     *         if parsing parameter fails.
     */
    public ChartDimension getChartDimension() {
        return new ChartDimension(getWidth(), getHeight());
    }

    /**
     * @return the requested chart width in pixels or the default {@value #DEFAULT_WIDTH}.
     * @throws IoParseException
     *         if parsing parameter fails.
     */
    private int getWidth() {
        if ( !query.containsKey(WIDTH)) {
            return DEFAULT_WIDTH;
        }
        return parseInteger(WIDTH);
    }

    /**
     * Returns the requested chart height in pixels.
     *
     * @return the requested chart height in pixels or the default {@value #DEFAULT_HEIGHT}.
     * @throws IoParseException
     *         if parsing parameter fails.
     */
    private int getHeight() {
        if ( !query.containsKey(HEIGHT)) {
            return DEFAULT_HEIGHT;
        }
        return parseInteger(HEIGHT);
    }

    /**
     * Indicates if rendered chart shall be returned as Base64 encoded string.
     *
     * @return the value of parameter {@value #BASE_64} or the default {@value #DEFAULT_BASE_64}.
     * @throws IoParseException
     *         if parsing parameter fails.
     */
    public boolean isBase64() {
        if ( !query.containsKey(BASE_64)) {
            return DEFAULT_BASE_64;
        }
        return parseBoolean(BASE_64);
    }

    /**
     * @return <code>true</code> if timeseries chart shall include a background grid.
     * @throws IoParseException
     *         if parsing parameter fails.
     */
    public boolean isGrid() {
        if ( !query.containsKey(GRID)) {
            return DEFAULT_GRID;
        }
        return parseBoolean(GRID);
    }

    /**
     * @return <code>true</code> if timeseries data shall be generalized.
     * @throws IoParseException
     *         if parsing parameter fails.
     */
    public boolean isGeneralize() throws IoParseException {
        if ( !query.containsKey(GENERALIZE)) {
            return DEFAULT_GENERALIZE;
        }
        return parseBoolean(GENERALIZE);
    }

    /**
     * @return <code>true</code> if a legend shall be included when rendering a chart, <code>false</code>
     *         otherwise.
     * @throws IoParseException
     *         if parsing parameter fails.
     */
    public boolean isLegend() {
        if ( !query.containsKey(LEGEND)) {
            return DEFAULT_LEGEND;
        }
        return parseBoolean(LEGEND);
    }

    /**
     * @return the value of {@value #LOCALE} parameter. If not present, the default {@value #DEFAULT_LOCALE}
     *         is returned.
     */
    public String getLocale() {
        if ( !query.containsKey(LOCALE)) {
            return DEFAULT_LOCALE;
        }
        return query.get(LOCALE);
    }

    /**
     * @return the value of {@value #STYLE} parameter. If not present, the default styles are returned.
     * @throws IoParseException
     *         if parsing style parameter failed.
     */
    public StyleProperties getStyle() {
        if ( !query.containsKey(STYLE)) {
            return StyleProperties.createDefaults();
        }
        return parseStyleProperties(query.get(STYLE));
    }

    /**
     * Creates a generic {@link StyleProperties} instance which can be used to create more concrete
     * {@link Style}s. For example use {@link LineStyle#createLineStyle(StyleProperties)} which gives you a
     * style view which can be used for lines.
     *
     * @param style
     *        the JSON style parameter to parse.
     * @return a parsed {@link StyleProperties} instance.
     * @throws IoParseException
     *         if parsing parameter fails.
     */
    private StyleProperties parseStyleProperties(String style) {
        try {
            return style == null ? StyleProperties.createDefaults()
                : new ObjectMapper().readValue(style, StyleProperties.class);
        }
        catch (JsonMappingException e) {
            throw new IoParseException("Could not read style properties: " + style, e);
        }
        catch (JsonParseException e) {
            throw new IoParseException("Could not parse style properties: " + style, e);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("An error occured during request handling.", e);
        }

    }

    public String getFormat() {
        if ( !query.containsKey(FORMAT)) {
            return DEFAULT_FORMAT;
        }
        return query.get(FORMAT);
    }

    /**
     * @return the value of {@value #TIMESPAN} parameter. If not present, the default timespan is returned.
     * @throws IoParseException
     *         if timespan could not be parsed.
     */
    public IntervalWithTimeZone getTimespan() {
        if ( !query.containsKey(TIMESPAN)) {
            return createDefaultTimespan();
        }
        return validateTimespan(query.get(TIMESPAN));
    }

    private IntervalWithTimeZone createDefaultTimespan() {
        DateTime now = new DateTime();
        DateTime lastWeek = now.minusWeeks(1);
        String interval = lastWeek
                .toString()
                .concat("/")
                .concat(now.toString());
        return new IntervalWithTimeZone(interval);
    }

    private IntervalWithTimeZone validateTimespan(String timespan) {
        return new IntervalWithTimeZone(timespan);
    }

    public Instant getResultTime() {
        if (!query.containsKey(RESULTTIME)) {
            return null;
        }
        return validateTimestamp(query.get(RESULTTIME));
    }

    private Instant validateTimestamp(String timestamp) {
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            String message = "Could not parse result time parameter." + timestamp;
            throw new IoParseException(message, e);
        }
    }

    public String getCategory() {
        return query.get(CATEGORY);
    }

    public String getService() {
        return query.get(SERVICE);
    }

    public String getOffering() {
        return query.get(OFFERING);
    }

    public String getFeature() {
        return query.get(FEATURE);
    }

    public String getProcedure() {
        return query.get(PROCEDURE);
    }

    public String getPhenomenon() {
        return query.get(PHENOMENON);
    }

    public String getStation() {
        return query.get(STATION);
    }

    /**
     * Creates a {@link BoundingBox} instance from given spatial request parameters. The resulting bounding
     * box is the merged extent of all spatial filters given. For example if {@value #NEAR} and {@value #BBOX}
     * exist, the returned bounding box includes both extents.
     *
     * @return a spatial filter created from given spatial parameters.
     * @throws IoParseException
     *         if parsing parameters fails, or if a requested {@value #CRS} object could not be created.
     */
    public BoundingBox getSpatialFilter() {
        if ( !query.containsKey(NEAR) && !query.containsKey(BBOX)) {
            return null;
        }

        BBox bboxBounds = createBbox();
        BoundingBox bounds = parseBoundsFromVicinity();
        return mergeBounds(bounds, bboxBounds);
    }

    private BoundingBox mergeBounds(BoundingBox bounds, BBox bboxBounds) {
        if (bboxBounds == null) {
            // nothing to merge
            return bounds;
        }
        CRSUtils crsUtils = createEpsgForcedXYAxisOrder();
        Point lowerLeft = crsUtils.convertToPointFrom(bboxBounds.getLl());
        Point upperRight = crsUtils.convertToPointFrom(bboxBounds.getUr());
        if (bounds == null) {
            bounds = new BoundingBox(lowerLeft, upperRight, DEFAULT_CRS);
            LOGGER.debug("Parsed bbox bounds: {}", bounds.toString());
        }
        else {
            bounds.extendBy(lowerLeft);
            bounds.extendBy(upperRight);
            LOGGER.debug("Merged bounds: {}", bounds.toString());
        }
        return bounds;
    }

    /**
     * @return a {@link BBox} instance or <code>null</code> if no {@link #BBOX} parameter is present.
     * @throws IoParseException
     *         if parsing parameter fails.
     * @throws IoParseException
     *         if a requested {@value #CRS} object could not be created
     */
    private BBox createBbox() {
        if ( !query.containsKey(BBOX)) {
            return null;
        }
        String bboxValue = query.get(BBOX);
        BBox bbox = parseJson(bboxValue, BBox.class);
        bbox.setLl(convertToCrs84(bbox.getLl()));
        bbox.setUr(convertToCrs84(bbox.getUr()));
        return bbox;
    }

    private BoundingBox parseBoundsFromVicinity() {
        if ( !query.containsKey(NEAR)) {
            return null;
        }
        String vicinityValue = query.get(NEAR);
        Vicinity vicinity = parseJson(vicinityValue, Vicinity.class);
        if (query.containsKey(CRS)) {
            vicinity.setCenter(convertToCrs84(vicinity.getCenter()));
        }
        BoundingBox bounds = vicinity.calculateBounds();
        LOGGER.debug("Parsed vicinity bounds: {}", bounds.toString());
        return bounds;
    }

    /**
     * @param jsonString
     *        the JSON string to parse.
     * @param clazz
     *        the type to serialize given JSON string to.
     * @return a mapped instance parsed from JSON.
     * @throws IoParseException
     *         if JSON is invalid or does not map to given type.
     */
    private <T> T parseJson(String jsonString, Class<T> clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, clazz);
        }
        catch (JsonParseException e) {
            throw new IoParseException("The given parameter is invalid JSON." + jsonString, e);
        }
        catch (JsonMappingException e) {
            throw new IoParseException("The given parameter could not been read: " + jsonString, e);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not handle input to parse.", e);
        }
    }

    private GeojsonPoint convertToCrs84(GeojsonPoint point) {
        return isForceXY() // is strict XY axis order?!
            ? transformToInnerCrs(point, createEpsgForcedXYAxisOrder())
            : transformToInnerCrs(point, createEpsgStrictAxisOrder());
    }

    /**
     * @param point
     *        a GeoJSON point to be transformed to internally used CRS:84.
     * @param crsUtils
     *        a reference helper.
     * @return a transformed GeoJSON instance.
     * @throws IoParseException
     *         if point could not be transformed, or if requested CRS object could not be created.
     */
    private GeojsonPoint transformToInnerCrs(GeojsonPoint point, CRSUtils crsUtils) {
        try {
            Point toTransformed = crsUtils.convertToPointFrom(point, getCrs());
            Point crs84Point = crsUtils.transformOuterToInner(toTransformed, getCrs());
            return crsUtils.convertToGeojsonFrom(crs84Point);
        }
        catch (TransformException e) {
            throw new IoParseException("Could not transform to internally used CRS:84.", e);
        }
        catch (FactoryException e) {
            throw new IoParseException("Check if 'crs' parameter is a valid EPSG CRS. Was: '" + getCrs() + "'.", e);
        }
    }

    /**
     * @return the requested reference context, or the default ({@value #DEFAULT_CRS} which will be
     *         interpreted as lon/lat ordered axes).
     */
    public String getCrs() {
        if ( !query.containsKey(CRS)) {
            return DEFAULT_CRS;
        }
        return query.get(CRS);
    }

    public boolean isForceXY() {
        if ( !query.containsKey(FORCE_XY)) {
            return DEFAULT_FORCE_XY;
        }
        return parseBoolean(FORCE_XY);
    }

    /**
     * @return the value of {@value #EXPANDED} parameter.
     * @throws IoParseException
     *         if parameter could not be parsed.
     */
    public boolean isExpanded() {
        if ( !query.containsKey(EXPANDED)) {
            return DEFAULT_EXPANDED;
        }
        return parseBoolean(EXPANDED);
    }

    public boolean isForceLatestValueRequests() {
        if ( !query.containsKey(FORCE_LATEST_VALUE)) {
            return DEFAULT_FORCE_LATEST_VALUE;
        }
        return parseBoolean(FORCE_LATEST_VALUE);
    }

    public boolean isStatusIntervalsRequests() {
    	if ( !query.containsKey(STATUS_INTERVALS)) {
    		return DEFAULT_STATUS_INTERVALS;
    	}
    	return parseBoolean(STATUS_INTERVALS);
    }

    public boolean isRenderingHintsRequests() {
    	if ( !query.containsKey(RENDERING_HINTS)) {
    		return DEFAULT_RENDERING_HINTS;
    	}
    	return parseBoolean(RENDERING_HINTS);
    }

    public boolean containsParameter(String parameter) {
        return query.containsKey(parameter);
    }

    public String getOther(String parameter) {
        return query.get(parameter);
    }

    /**
     * @param parameter
     *        the parameter to parse to an <code>int</code> value.
     * @return an integer value.
     * @throws IoParseException
     *         if parsing to <code>int</code> fails.
     */
    private int parseInteger(String parameter) {
        try {
            String value = query.get(parameter);
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            throw new IoParseException("Parameter '" + parameter + "' has to be an integer!", e);
        }
    }

    /**
     * @param parameter
     *        the parameter to parse to <code>boolean</code>.
     * @return <code>true</code> or <code>false</code> as <code>boolean</code>.
     * @throws IoParseException
     *         if parsing to <code>boolean</code> fails.
     */
    private boolean parseBoolean(String parameter) {
        try {
            String value = query.get(parameter);
            return Boolean.parseBoolean(value);
        }
        catch (NumberFormatException e) {
            throw new IoParseException("Parameter '" + parameter + "' has to be 'false' or 'true'!", e);
        }
    }

    public UndesignedParameterSet toUndesignedParameterSet() {
        UndesignedParameterSet parameterSet = new UndesignedParameterSet();
        addValuesToParameterSet(parameterSet);
        return parameterSet;
    }

    public DesignedParameterSet toDesignedParameterSet() {
        DesignedParameterSet parameterSet = new DesignedParameterSet();
        addValuesToParameterSet(parameterSet);
        return parameterSet;
    }

    private ParameterSet addValuesToParameterSet(ParameterSet parameterSet) {
        for (Entry<String, String> entry : query.entrySet()) {
            parameterSet.addParameter(entry.getKey().toLowerCase(), entry.getValue());
        }
        return parameterSet;
    }


    public static IoParameters createDefaults() {
        return new IoParameters(null);
    }

    /**
     * @param queryParameters
     *        the parameters sent via GET payload.
     * @return a query map for convenient parameter access plus validation.
     */
    public static IoParameters createFromQuery(Map<String, String> queryParameters) {
        return new IoParameters(queryParameters);
    }

    /**
     * @param parameters
     *        the parameters sent via POST payload.
     * @return a query map for convenient parameter access plus validation.
     */
    public static IoParameters createFromQuery(ParameterSet parameters) {
        return createFromQuery(createQueryParametersFrom(parameters));
    }

    private static Map<String, String> createQueryParametersFrom(ParameterSet parameters) {
        Map<String, String> queryParameters = new HashMap<String, String>();
        for (String parameter : parameters.availableParameters()) {
            Object value = parameters.getAsObject(parameter);
            queryParameters.put(parameter.toLowerCase(), String.valueOf(value));
        }
        return queryParameters;
    }

}
