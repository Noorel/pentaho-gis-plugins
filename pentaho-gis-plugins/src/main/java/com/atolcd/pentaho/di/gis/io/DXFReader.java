package com.atolcd.pentaho.di.gis.io;

/*
 * #%L
 * Pentaho Data Integrator GIS Plugin
 * %%
 * Copyright (C) 2015 Atol CD
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.exception.KettleException;

import com.atolcd.gis.dxf.Entity;
import com.atolcd.gis.dxf.Layer;
import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;

public class DXFReader extends AbstractFileReader {

    private String dxfFileName;
    private boolean dxfFileExist;
    private boolean circleAsPolygon;
    private boolean ellipseAsPolygon;
    private boolean lineAsPolygon;

    public DXFReader(String fileName, String geometryFieldName, String charsetName, boolean circleAsPolygon, boolean ellipseAsPolygon, boolean lineAsPolygon)
            throws KettleException {
        super(null, geometryFieldName, charsetName);

        try {
            this.dxfFileExist = new File(checkFilename(fileName).getFile()).exists();

            if (!this.dxfFileExist) {
                throw new KettleException("Missing " + fileName + " file");
            } else {
                this.dxfFileName = checkFilename(fileName).getFile();
            }

            this.fields.add(new Field(geometryFieldName, FieldType.GEOMETRY, null, null));
            this.fields.add(new Field("Layer", FieldType.STRING, null, null));
            this.fields.add(new Field("Text", FieldType.STRING, null, null));

            this.circleAsPolygon = circleAsPolygon;
            this.ellipseAsPolygon = ellipseAsPolygon;
            this.lineAsPolygon = lineAsPolygon;

        } catch (Exception e) {
            throw new KettleException("Error initialize reader", e);
        }
    }

    public List<Feature> getFeatures() throws KettleException {
        List<Feature> features = new ArrayList<Feature>();

        try {
            com.atolcd.gis.dxf.DXFReader reader = new com.atolcd.gis.dxf.DXFReader(this.dxfFileName);
            reader.setCircleAsPolygon(this.circleAsPolygon);
            reader.setEllipseAsPolygon(this.ellipseAsPolygon);
            reader.setPolylineAsPolygon(this.lineAsPolygon);

            int entitiesTotalNumber = 0;
            for (Layer layer : reader.getLayers()) {
                entitiesTotalNumber += layer.getEntities().size();
            }

            if (this.limit == 0 || this.limit > entitiesTotalNumber || this.limit < 0) {
                this.limit = entitiesTotalNumber;
            }

            int entityNumber = 0;

            for (Layer layer : reader.getLayers()) {
                for (Entity entity : layer.getEntities()) {
                    if (entityNumber < this.limit) {
                        Feature feature = new Feature();

                        if (this.forceToMultiGeometry) {
                            feature.addValue(this.fields.get(0), GeometryUtils.getMultiGeometry(entity.getGeometry()));
                        } else {
                            feature.addValue(this.fields.get(0), entity.getGeometry());
                        }

                        feature.addValue(this.fields.get(0), entity.getGeometry());
                        feature.addValue(this.fields.get(1), layer.getName());
                        feature.addValue(this.fields.get(2), entity.getText());

                        features.add(feature);
                        entityNumber += 1;
                    } else {
                        break;
                    }
                }

                if (entityNumber > this.limit) {
                    break;
                }
            }

        } catch (Exception e) {
            throw new KettleException("Error reading features" + this.dxfFileName, e);
        }
        return features;
    }
}