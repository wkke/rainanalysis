package com.wkken.rainanalysis;

import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;

public class ShpUtils {
public static SimpleFeatureCollection readShp(String path ){
    return readShp(path, null);

}

public static SimpleFeatureCollection  readShp(String path , Filter filter){

    SimpleFeatureSource  featureSource = readStoreByShp(path);

    if(featureSource == null) return null;

    try {
        return filter != null ? featureSource.getFeatures(filter) : featureSource.getFeatures() ;
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }

    return null ;
}

public static SimpleFeatureSource readStoreByShp(String path ){

    File file = new File(path);

    FileDataStore store;
    SimpleFeatureSource featureSource = null;
    try {
        store = FileDataStoreFinder.getDataStore(file);
        ((ShapefileDataStore) store).setCharset(Charset.forName("UTF-8"));
        featureSource = store.getFeatureSource();
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }

    return featureSource ;
}

public static void transShape(String srcfilepath, String destfilepath, Collection<Geometry> geos,SimpleFeatureType sft) {
    try {

        //源shape文件
        ShapefileDataStore shapeDS = (ShapefileDataStore) new ShapefileDataStoreFactory().createDataStore(new File(srcfilepath).toURI().toURL());
        //创建目标shape文件对象
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
        params.put(ShapefileDataStoreFactory.URLP.key, new File(destfilepath).toURI().toURL());
        ShapefileDataStore ds = (ShapefileDataStore) factory.createNewDataStore(params);
        // 设置属性
        SimpleFeatureSource fs = shapeDS.getFeatureSource(shapeDS.getTypeNames()[0]);
        //下面这行还有其他写法，根据源shape文件的simpleFeatureType可以不用retype，而直接用fs.getSchema设置
        ds.createSchema(SimpleFeatureTypeBuilder.retype(fs.getSchema(), DefaultGeographicCRS.WGS84));

        //设置writer
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(ds.getTypeNames()[0], Transaction.AUTO_COMMIT);



        //写记录
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(sft);


        for (Iterator<Geometry> i = geos.iterator(); i.hasNext(); ) {
            Geometry geometry = i.next();

            Object[] obj = {geometry, 0,0,0,0,0,0,null};
            SimpleFeature f = featureBuilder.buildFeature(null, obj);

            SimpleFeature fNew = writer.next();
            fNew.setAttributes(f.getAttributes());
            writer.write();
        }
  /*      SimpleFeatureIterator it = fs.getFeatures().features();
        try {
            while (it.hasNext()) {
                SimpleFeature f = it.next();
                SimpleFeature fNew = writer.next();
                fNew.setAttributes(f.getAttributes());
                writer.write();
            }
        } finally {
            it.close();
        }*/
        writer.close();
        ds.dispose();
        shapeDS.dispose();
    } catch (Exception e) { e.printStackTrace();    }
}

public static void transShapelist(String srcfilepath, String destfilepath, List<Polygon> geos, SimpleFeatureType sft) {
    try {

        //源shape文件
        ShapefileDataStore shapeDS = (ShapefileDataStore) new ShapefileDataStoreFactory().createDataStore(new File(srcfilepath).toURI().toURL());
        //创建目标shape文件对象
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
        params.put(ShapefileDataStoreFactory.URLP.key, new File(destfilepath).toURI().toURL());
        ShapefileDataStore ds = (ShapefileDataStore) factory.createNewDataStore(params);
        // 设置属性
        SimpleFeatureSource fs = shapeDS.getFeatureSource(shapeDS.getTypeNames()[0]);
        //下面这行还有其他写法，根据源shape文件的simpleFeatureType可以不用retype，而直接用fs.getSchema设置
        ds.createSchema(SimpleFeatureTypeBuilder.retype(fs.getSchema(), DefaultGeographicCRS.WGS84));

        //设置writer
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(ds.getTypeNames()[0], Transaction.AUTO_COMMIT);



        //写记录
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(sft);


        for (Iterator<Polygon> i = geos.iterator(); i.hasNext(); ) {
            Geometry geometry = i.next();

            Object[] obj = {geometry, 0,0,0,0,0,0,null};
            SimpleFeature f = featureBuilder.buildFeature(null, obj);

            SimpleFeature fNew = writer.next();
            fNew.setAttributes(f.getAttributes());
            writer.write();
        }
  /*      SimpleFeatureIterator it = fs.getFeatures().features();
        try {
            while (it.hasNext()) {
                SimpleFeature f = it.next();
                SimpleFeature fNew = writer.next();
                fNew.setAttributes(f.getAttributes());
                writer.write();
            }
        } finally {
            it.close();
        }*/
        writer.close();
        ds.dispose();
        shapeDS.dispose();
    } catch (Exception e) { e.printStackTrace();    }
}

}
