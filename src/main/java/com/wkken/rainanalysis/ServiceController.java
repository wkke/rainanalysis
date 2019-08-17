package com.wkken.rainanalysis;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class ServiceController {
@RequestMapping(value = "/rainwarn", method = RequestMethod.GET)
public String rainwarn(){


    Record r= Db.findFirst("SELECT top 1 * FROM [WeatherWarn]  order by UpdateDate desc  ");
    return r.getStr("GeoData");


}



}
