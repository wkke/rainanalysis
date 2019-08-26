package com.wkken.rainanalysis;

import cn.hutool.core.lang.Console;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
public class ServiceController {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
@RequestMapping(value = "/rainwarn", method = RequestMethod.GET)
public String rainwarn(@RequestParam(name = "tm") Optional<String> tm) throws ParseException {

    Record r=null;
String sql="SELECT top 1 * FROM [WeatherWarn]  order by UpdateDate desc  ";

    if (tm.isPresent())
    {
        Date tmd=sdf.parse(tm.get());
        Timestamp sqlDate = new Timestamp(tmd.getTime());//uilt date转sql date

        r= Db.findFirst("SELECT top 1 * FROM [WeatherWarn]    order by  abs(datediff(minute,tm,?))   asc",sqlDate);

    }else
    {
        r= Db.findFirst("SELECT top 1 * FROM [WeatherWarn]  order by UpdateDate desc  ");
    }




    return r.getStr("GeoData");


}



}
