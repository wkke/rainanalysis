package com.wkken.rainanalysis;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.jfinal.plugin.activerecord.Db;
import com.wkken.grib2tools.grib2file.GribSection1;
import com.wkken.grib2tools.grib2file.RandomAccessGribFile;
import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationHome;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import ucar.ma2.Array;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class WarnUtils {
private static final Logger log = Logger.getLogger(WarnUtils.class);
public String GribFilePath = "D:\\work\\testdir\\Z_NWGD_C_BCCD_20190725153816_P_RFFC_SPCC-ER03_201907252000_07203.GRB2";
//D:\work\南沙\Z_NWGD_C_BCCD_20190704052941_P_RFFC_SPCC-ER03_201907040800_07203.GRB2
@Value("${GribFileDir}")
public String GribFileDir = "D:\\work\\testdir";

private   Double[][] tabledata=new Double[170][229];

private Table<Double, Double, Double> rainGridTable = HashBasedTable.create();



private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


private Map<String, Object> StyleMap = ImmutableMap.<String, Object>builder()

        .put("red", ImmutableMap.<String, Object>builder()
                .put("warnlevel", "red")
                .put("color","#ff0000")
                .put("opacity",0.3)
                .put("weight",0)
                .put("fill", "#ff0000")
                .put("fill-opacity", 0.3)
                .put("stroke-width", 0)
                .build()
        )
        .put("orange", ImmutableMap.<String, Object>builder()
                .put("warnlevel", "orange")
                .put("color","#ff8000")
                .put("opacity",0.3)
                .put("weight",0)
                .put("fill", "#ff8000")
                .put("fill-opacity", 0.3)
                .put("stroke-width", 0)
                .build()
        )
        .put("yellow", ImmutableMap.<String, Object>builder()
                .put("warnlevel", "yellow")
                .put("color","#f3f920")
                .put("opacity",0.3)
                .put("weight",0)
                .put("fill", "#f3f920")
                .put("fill-opacity", 0.3)
                .put("stroke-width", 0)
                .build()
        )
        .put("blue", ImmutableMap.<String, Object>builder()
                .put("warnlevel", "blue")
                .put("color","#0080ff")
                .put("opacity",0.3)
                .put("weight",0)
                .put("fill", "#0080ff")
                .put("fill-opacity", 0.3)
                .put("stroke-width", 0)
                .build()
        )
        .build();


private String stm = "";
private String etm = "";
private double maxRainFall = 0.0;

private String errmessage = "";

    private  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

public static void main(String[] args) throws Exception {
    StopWatch sw = new StopWatch();

    sw.start("开始计算");
    WarnUtils wu = new WarnUtils();
    sw.stop();
    System.out.println(sw.prettyPrint());
    System.out.println(sw.getTotalTimeMillis());
    System.out.println(ResultHolder.INSTANCE.getResult());


//    wu.fetchRainValue(29.35,102.65);
   // 102.65  ,
   // wu.checkfileDate();
    //   wu.updateResult();
   // System.out.println( JSON.toJSONString(wu.DoAnalyResult())); ;
}




@Scheduled(fixedRate=1000*60*30)
public void updateResult2() throws ParseException {

    log.error("检查文件并处理结果");
    List<File> files=checkFiles();

    if (files.size()==0)
    {
        log.error("未发现预报文件");
    }else {

GridUtils gu=new GridUtils();

        Map<String, Object> forecastdata;
        for (File grbfile : files) {

            try {
                if (needCalc(grbfile))
                {
                    forecastdata= gu.analysis(grbfile);


                    String updatesql="UPDATE [WeatherWarn] SET [TM] = ? , [UpdateDate] = ? ,[GeoData] = ? WHERE TM =? and  UpdateDate <?";
                    Db.update(updatesql,new Timestamp(df.parse(forecastdata.get("forecast_stm").toString()).getTime())  ,
                            new Timestamp(df.parse(forecastdata.get("updatetime").toString()).getTime()),
                            JSON.toJSONString(forecastdata),
                            new Timestamp(df.parse(forecastdata.get("forecast_stm").toString()).getTime()),
                            new Timestamp(df.parse(forecastdata.get("updatetime").toString()).getTime())
                    );

                    String insertsql="if not exists (select 1 from WeatherWarn where  TM =? )\n" +
                                    "INSERT INTO [dbo].[WeatherWarn]\n" +
                                    "           ([TM]\n" +
                                    "           ,[UpdateDate]\n" +
                                    "           ,[GeoData])\n" +
                                    "     VALUES\n" +
                                    "           (?\n" +
                                    "           ,?\n" +
                                    "           ,?)";
                    Db.update(insertsql,  new Timestamp(df.parse(forecastdata.get("forecast_stm").toString()).getTime()),

                            new Timestamp(df.parse(forecastdata.get("forecast_stm").toString()).getTime()),
                            new Timestamp(df.parse(forecastdata.get("updatetime").toString()).getTime()) ,
                            JSON.toJSONString(forecastdata)
                    );



                }
            } catch (IOException e) {
                e.printStackTrace();
            }




        }

        log.error("--process over---");
    }

}

private  boolean needCalc(File f) throws IOException, ParseException {

    try {
        return  0==  Db.queryInt("select count(1) from WeatherWarn where  TM =? and  UpdateDate =?",    new Timestamp(df.parse(GridUtils.checkForeCastDate(f)).getTime()),     new Timestamp(f.lastModified())  );
    } catch (ParseException e) {
        e.printStackTrace();
        return true;
    }

}





public  List<File> checkFiles() {
    List<File> fs = new ArrayList<>();
    log.error("预报文件目录:" + GribFileDir);
    File filedir = new File(GribFileDir);
    if (filedir.exists() && filedir.isDirectory()) {

        //两次获取 仅取修改时间距当前时间3小时以内的
        Collections.addAll(fs, filedir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) { //仅监视.GRB2格式文件

                if ((new Date().getTime() - file.lastModified()) < (1000 * 60 *60* ResultHolder.INSTANCE.getHours()) && (s.toLowerCase().endsWith(".grb2"))) {
                    return true;
                }
                return false;
            }
        }));

        fs = new Ordering<File>() {
            @Override
            public int compare(File left, File right) {
                if (left.lastModified() > right.lastModified()) {
                    return -1;
                }
                if (left.lastModified() == right.lastModified()) {
                    return 0;
                }
                if (left.lastModified() < right.lastModified()) {
                    return 1;
                }
                return 0;
            }
        }.immutableSortedCopy(fs);

        log.error("---------------有效预报文件数量:" + fs.size());
        if (fs.size() == 0) {

        }

    }
    return fs;
}




public Map<String, Object> DoAnalyResult() {
    Map<String, Object> ret = new HashMap<String, Object>();


    try {
        initRainValue();
        ret.put("features", calcWarn());

    } catch (Exception e) {
        e.printStackTrace();

        errmessage = e.getMessage();
        log.error("分析过程发生异常:"+errmessage);
    }

    ret.put("type", "FeatureCollection");
    ret.put("forecast_stm", stm);
    ret.put("forecast_etm", etm);
    ret.put("msg", errmessage);
    ret.put("maxrain", maxRainFall);


    return ret;
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



    maxRainFall=0.0;
    SimpleFeature feature;

    SimpleFeatureCollection colls1 = ShpUtils.readShp(filepath);
    SimpleFeatureIterator iters = colls1.features();
    while(iters.hasNext()){
        feature = iters.next();
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

private Map<String, Object> makeWarnLevel(String warnlevel, Collection<Geometry> arealist) throws IOException {

    HashMap<String, Object> ret = new HashMap<>();
    Geometry all = null;
    for (Iterator<Geometry> i = arealist.iterator(); i.hasNext(); ) {
        Geometry geometry = i.next();
        if (geometry == null) continue;
        if (all == null) {
            all = geometry;
        } else {
            all = all.union(geometry);
        }
    }
    MultiPolygon mp = (MultiPolygon) all;


    List<Polygon> polys = new ArrayList<>();

    int p = mp.getNumGeometries();


    for (int i = 0; i < p; i++) {
        if (mp.getGeometryN(i).getArea() > 0.01) {
            polys.add((Polygon) mp.getGeometryN(i));
        }
    }

    MultiPolygon targetMP = new MultiPolygon(polys.toArray(new Polygon[0]), all.getFactory());
    StringWriter writer = new StringWriter();
    GeometryJSON g = new GeometryJSON();
    g.write(targetMP, writer);
    ret.put("warnlevel", warnlevel);

    ret.put("type", "Feature");
    ret.put("geometry", JSON.parse(writer.toString()));
    ret.put("properties", StyleMap.get(warnlevel));
    ret.put("style", StyleMap.get(warnlevel));

    return ret;
}

private void initRainValue() throws Exception {
log.error("初始化雨量预报网格数据,预报文件为:"+GribFilePath);
    //读取预报起止时间
    InputStream inputstream;
    RandomAccessGribFile gribFile;
    inputstream = Files.newInputStream(Paths.get(GribFilePath));
    gribFile = new RandomAccessGribFile("testdata", GribFilePath);
    gribFile.importFromStream(inputstream, 0);
    GribSection1 section1 = gribFile.getSection1();
    stm = String.valueOf(section1.year) + "-" + String.format("%02d", section1.month) + "-" + String.format("%02d", section1.day) + " " + String.format("%02d", section1.hour) + ":" + String.format("%02d", section1.minute) + ":" + String.format("%02d", section1.second);
    Date startDate = df.parse(stm);
    startDate = new Date(startDate.getTime() + 1000 * 60 * 60 * 72);
    etm = df.format(startDate);
    inputstream.close();
    //解析并建立降雨数据网格
    AccessGRBData grbData = new AccessGRBData(GribFilePath);
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
                if (rainGridTable.contains(lat, lgt)) {

                    rainGridTable.put(lat, lgt, rainGridTable.get(lat, lgt) + frame[xd][yd]);
                } else {
                    rainGridTable.put(lat, lgt, Double.valueOf(frame[xd][yd]));
                }
            }
        }

        dayindex++;
    }
    grbData.Dispose();

}


private void initRainValue2() throws Exception {

    //读取预报起止时间
    InputStream inputstream;
    RandomAccessGribFile gribFile;
    inputstream = Files.newInputStream(Paths.get(GribFilePath));
    gribFile = new RandomAccessGribFile("testdata", GribFilePath);
    gribFile.importFromStream(inputstream, 0);
    GribSection1 section1 = gribFile.getSection1();
    stm = String.valueOf(section1.year) + "-" + String.format("%02d", section1.month) + "-" + String.format("%02d", section1.day) + " " + String.format("%02d", section1.hour) + ":" + String.format("%02d", section1.minute) + ":" + String.format("%02d", section1.second);
    Date startDate = df.parse(stm);
    startDate = new Date(startDate.getTime() + 1000 * 60 * 60 * 72);
    etm = df.format(startDate);
    inputstream.close();
    //解析并建立降雨数据网格
    AccessGRBData grbData = new AccessGRBData(GribFilePath);
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

   Double[][] tabledata=new Double[170][229];

    for (float[][] frame : gridvalues) {  //24个周期的数据
        for (int xd = 0; xd < frame.length; xd++) {
            for (int yd = 0; yd < frame[xd].length; yd++) {

                tabledata[xd][yd]=tabledata[xd][yd]+frame[xd][yd];

            }
        }
    }
    grbData.Dispose();


}

private  double minx=26.05;
private  double miny=97.30;

private double fetchRainValue(double lat, double lgt) {

/*    Double xd = null;
    Double yd = null;
    Iterator<Double> xit = rainGridTable.rowKeySet().iterator();
    while (xit.hasNext()) {
        xd = xit.next();
        if (abs(lat - xd) < 0.025 || abs(lat - xd) == 0.025) {
            break;
        }
    }
    Iterator<Double> yit = rainGridTable.columnKeySet().iterator();
    while (yit.hasNext()) {
        yd = yit.next();
        if (abs(lgt - yd) < 0.025 || abs(lgt - yd) == 0.025) {
            break;
        }
    }*/

  //  return rainGridTable.get(xd, yd);


    //计算出最近的网格
    Double p=Math.floor((lat-minx)/0.05);
  int rowIndex=p.intValue();

     p=Math.floor((lgt-miny)/0.05);
    int columnIndex=p.intValue();
    return  tabledata[rowIndex][columnIndex];

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
