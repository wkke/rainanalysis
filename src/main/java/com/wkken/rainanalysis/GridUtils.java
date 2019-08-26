package com.wkken.rainanalysis;

import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.wkken.grib2tools.grib2file.GribSection1;
import com.wkken.grib2tools.grib2file.RandomAccessGribFile;
import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.boot.ApplicationHome;
import ucar.ma2.Array;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class GridUtils {
private static final Logger log = Logger.getLogger(GridUtils.class);

private List<SimpleFeature> geolist=new ArrayList<>();
private Map<String, Object> StyleMap = ImmutableMap.<String, Object>builder()

        /* "color": "#CC0000",
                 "opacity": 1,
                 "weight": 4 }*/

        .put("red", ImmutableMap.<String, Object>builder()
                .put("warnlevel", "red")
                .put("color","#ff0000")
                .put("opacity",0.7)
                .put("weight",2)
                .put("fill", "#ff0000")
                .put("fill-opacity", 0.7)
                .put("stroke-width", 0)
                .build()
        )
        .put("orange", ImmutableMap.<String, Object>builder()
                .put("warnlevel", "orange")
                .put("color","#ff8000")
                .put("opacity",0.7)
                .put("weight",2)
                .put("fill", "#ff8000")
                .put("fill-opacity", 0.7)
                .put("stroke-width", 0)
                .build()
        )
        .put("yellow", ImmutableMap.<String, Object>builder()
                .put("warnlevel", "yellow")
                .put("color","#f3f920")
                .put("opacity",0.7)
                .put("weight",2)
                .put("fill", "#f3f920")
                .put("fill-opacity", 0.7)
                .put("stroke-width", 0)
                .build()
        )
        .put("blue", ImmutableMap.<String, Object>builder()
                .put("warnlevel", "blue")
                .put("color","#0080ff")
                .put("opacity",0.7)
                .put("weight",2)
                .put("fill", "#0080ff")
                .put("fill-opacity", 0.7)
                .put("stroke-width", 0)
                .build()
        )
        .build();



private String stm = "";
private String etm = "";
private double maxRainFall = 0.0;
private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

private   Double[][] tabledata=new Double[170][229];
    public   Map<String, Object> analysis(File gribfile)
    {
        Map<String, Object> ret = new HashMap<String, Object>();

        try {
            initRainValue(gribfile);
            ret.put("features", calcWarn());
            log.error("文件处理完毕:"+gribfile.getName());

        } catch (Exception e) {
            e.printStackTrace();
            log.error("分析过程发生异常:"+e.getMessage());
        }

        ret.put("type", "FeatureCollection");
        ret.put("forecast_stm", stm);
        ret.put("forecast_etm", etm);


        ret.put("updatetime",df.format(new Date(gribfile.lastModified())));
        ret.put("maxrain", maxRainFall);

        return ret;

    }

    public static String  checkForeCastDate( File gribfile) throws IOException {
        String ret="";
        InputStream inputstream;
        RandomAccessGribFile gribFile;
        inputstream = Files.newInputStream(Paths.get(gribfile.getAbsolutePath()));
        gribFile = new RandomAccessGribFile("testdata", gribfile.getAbsolutePath());
        gribFile.importFromStream(inputstream, 0);
        GribSection1 section1 = gribFile.getSection1();
        ret = String.valueOf(section1.year) + "-" + String.format("%02d", section1.month) + "-" + String.format("%02d", section1.day) + " " + String.format("%02d", section1.hour) + ":" + String.format("%02d", section1.minute) + ":" + String.format("%02d", section1.second)+".000";

        inputstream.close();
         return ret;
    }

    private  void  initRainValue(File gribfile) throws Exception {
        log.error("初始化雨量预报网格数据,预报文件为:"+gribfile.getAbsolutePath());
        //读取预报起止时间
        InputStream inputstream;
        RandomAccessGribFile gribFile;
        inputstream = Files.newInputStream(Paths.get(gribfile.getAbsolutePath()));
        gribFile = new RandomAccessGribFile("testdata", gribfile.getAbsolutePath());
        gribFile.importFromStream(inputstream, 0);
        GribSection1 section1 = gribFile.getSection1();
        stm = String.valueOf(section1.year) + "-" + String.format("%02d", section1.month) + "-" + String.format("%02d", section1.day) + " " + String.format("%02d", section1.hour) + ":" + String.format("%02d", section1.minute) + ":" + String.format("%02d", section1.second)+".000";
        Date startDate = df.parse(stm);
        startDate = new Date(startDate.getTime() + 1000 * 60 * 60 * 72);
        etm = df.format(startDate)+".000";
        inputstream.close();
        //解析并建立降雨数据网格
        AccessGRBData grbData = new AccessGRBData(gribfile.getAbsolutePath());
        grbData.ReadFile();
        List<GridDatatype> grids = grbData.getGrids();
        float[][][] gridvalues = null;
        VariableDS var = grids.get(0).getVariable();
        int[] shape = var.getShape();
        int[] origin = new int[shape.length];
        gridvalues = getDataArray(var.read(origin, shape).reduce());

        Double lgt;
        Double lat;

        float maxRain=0;

        tabledata= new Double[170][229];

        int dayindex=0;
        for (float[][] frame : gridvalues) {  //24个周期的数据
            if (dayindex==8){ break;} //仅获取24小时数据
            for (int xd = 0; xd < frame.length; xd++) {
                for (int yd = 0; yd < frame[xd].length; yd++) {
                    tabledata[xd][yd]=(tabledata[xd][yd]==null)?frame[xd][yd]:tabledata[xd][yd]+frame[xd][yd];


                    if (maxRain< frame[xd][yd])
                    {
                        maxRain=frame[xd][yd];
                    }
                    lat = Double.valueOf(26.00 + xd * 0.05);
                    lgt = Double.valueOf(97.25 + yd * 0.05);
                }
            }

            dayindex++;
        }
        grbData.Dispose();
    }
private List<Map<String, Object>> calcWarn() throws IOException, InterruptedException {
    List<Map<String, Object>> ret = new ArrayList<>();

    ApplicationHome home = new ApplicationHome(getClass());

    String filepath=home.getDir()+"/shp/rainwarn.shp";

    log.error("加载预警分区，文件为:"+filepath);
    Collection<Geometry> redlist = new ArrayList<Geometry>();
    Collection<Geometry> bluelist = new ArrayList<Geometry>();
    Collection<Geometry> yellowlist = new ArrayList<Geometry>();
    Collection<Geometry> orengelist = new ArrayList<Geometry>();

    double bluev = 0.00;
    double yellowv = 0.00;
    double orangev = 0.00;
    double redv = 0.00;

    double lat = 0;
    double lgt = 0;

    log.error("预警分区读取完毕，开始分组");

    maxRainFall=0.0;
    SimpleFeature feature;


    if(geolist.size()==0)
    {
        geolist=ShpUtils.getRegionList(filepath);
    }
  //  SimpleFeatureCollection colls1 = ShpUtils.readShp(filepath);
  //  SimpleFeatureIterator iters = colls1.features();

    for (SimpleFeature sf : geolist)
    {
        feature = sf;

  //  while(iters.hasNext()){
     //   feature = iters.next();





        bluev = Float.parseFloat(feature.getAttribute("v_blue").toString());
        yellowv = Float.parseFloat(feature.getAttribute("v_yellow").toString());
        orangev = Float.parseFloat(feature.getAttribute("v_orange").toString());
        redv =Float.parseFloat(feature.getAttribute("v_red").toString());

        lat = Double.valueOf(feature.getAttribute("center_lat").toString());
        lgt = Double.valueOf(feature.getAttribute("center_lgt").toString());


        double rainv = fetchRainValue(lat, lgt);
        if (maxRainFall<rainv)
        {
            maxRainFall=rainv;
        }

        //最低蓝色预警值为25  故小于25直接略过；
        if (rainv<25)
        {
            continue;
        }

        if (rainv >= redv) {
            redlist.add((MultiPolygon) feature.getDefaultGeometry());
            continue;
        }
        if (rainv >= orangev) {
            orengelist.add((MultiPolygon) feature.getDefaultGeometry());
            continue;
        }
        if (rainv >= yellowv) {
            yellowlist.add((MultiPolygon) feature.getDefaultGeometry());
            continue;
        }
        if (rainv >= bluev) {
            bluelist.add((MultiPolygon) feature.getDefaultGeometry());
            continue;
        }

    }
//    iters.close();





    log.error("最大降雨"+maxRainFall);

    Map<String, Object> mp = null;
    log.error("处理红色预警区域，数量为"+redlist.size());
    if (redlist.size()>0)
    {
        mp = makeWarnLevel("red", redlist);
        if (mp.containsKey("geometry")) {
            ret.add(mp);
        }
    }
    log.error("处理橙色预警区域，数量为"+orengelist.size());
    if (orengelist.size()>0) {
        mp = makeWarnLevel("orange", orengelist);
        if (mp.containsKey("geometry")) {
            ret.add(mp);
        }
    }
    log.error("处理黄色预警区域，数量为"+yellowlist.size());
    if (yellowlist.size()>0)
    {
        mp = makeWarnLevel("yellow", yellowlist);
        if (mp.containsKey("geometry")) {
            ret.add(mp);
        }}
    log.error("处理蓝色预警区域，数量为"+bluelist.size());
    if (bluelist.size()>0) {
        mp = makeWarnLevel("blue", bluelist);
        if (mp.containsKey("geometry")) {
            ret.add(mp);
        }
    }
    return ret;
}

private  double minx=26.05;
private  double miny=97.30;

private double fetchRainValue(double lat, double lgt) {

    //计算出最近的网格
    Double p=Math.floor((lat-minx)/0.05);
    int rowIndex=p.intValue();

    p=Math.floor((lgt-miny)/0.05);
    int columnIndex=p.intValue();
    return  tabledata[rowIndex][columnIndex];

}
private SimpleFeatureType sft=null;

private Map<String, Object> makeWarnLevel(String warnlevel, Collection<Geometry> arealist) throws IOException {
    System.out.println("处理合并" + warnlevel + arealist.size());
    if (ResultHolder.INSTANCE.getDebug()) {
        ShpUtils.transShape("D:\\code\\java\\rainanalysis\\src\\main\\resources\\shp\\rainwarn.shp", "D:\\test\\new" + warnlevel + ".shp", arealist, sft);
    }
    HashMap<String, Object> ret = new HashMap<>();
    Geometry all = null;
    GeometryFactory gf = new GeometryFactory();
    for (Iterator<Geometry> i = arealist.iterator(); i.hasNext(); ) {
        Geometry geometry = i.next();
        if (geometry == null) continue;
        if (all == null) {

            if (geometry instanceof  Polygon){
                Polygon[] polys = new Polygon[1];
                polys[0] =(Polygon) geometry;
                all = gf.createMultiPolygon(polys);
            } else {
                all = geometry;
            }
        } else {
            all = all.union(geometry);

        }
    }


    if (all instanceof  Polygon){
        Polygon[] polys = new Polygon[1];
        polys[0] =(Polygon) all;
        all = gf.createMultiPolygon(polys);
    }
    MultiPolygon mp = (MultiPolygon) all;

    List<Polygon> polys = new ArrayList<>();

    int p = mp.getNumGeometries();
    for (int i = 0; i < p; i++) {
        if (mp.getGeometryN(i).getArea() > 0.01) {
            polys.add((Polygon) mp.getGeometryN(i));
        }
    }

    if (ResultHolder.INSTANCE.getDebug())
    {
        ShpUtils.transShapelist("D:\\code\\java\\rainanalysis\\src\\main\\resources\\shp\\rainwarn.shp","D:\\test\\newlsit"+warnlevel+".shp",polys,sft);
    }



    MultiPolygon targetMP = new MultiPolygon(polys.toArray(new Polygon[0]), all.getFactory());
    StringWriter writer = new StringWriter();
    GeometryJSON g = new GeometryJSON();
    g.write(targetMP, writer);
    ret.put("warnlevel", warnlevel);

    ret.put("type", "Feature");
    ret.put("geometry", JSON.parse(writer.toString()));
    ret.put("properties", StyleMap.get(warnlevel));
 //   ret.put("style", StyleMap.get(warnlevel));

    return ret;
}
private float[][][] getDataArray(Array data2D) {
    float[][][] dataF = null;
    try {

        dataF = (float[][][]) data2D.copyToNDJavaArray();
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return dataF;
}
}
