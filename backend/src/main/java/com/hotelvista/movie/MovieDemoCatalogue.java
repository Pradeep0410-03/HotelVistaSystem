package com.hotelvista.movie;

import java.time.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Profile("demo")
public class MovieDemoCatalogue implements ApplicationRunner {
    private final JdbcTemplate db;
    public MovieDemoCatalogue(JdbcTemplate db){this.db=db;}
    @Override @Transactional public void run(ApplicationArguments args){
        // Serialize demo seeding across application instances without touching existing seat claims.
        db.execute("SELECT pg_advisory_xact_lock(86421051)");
        for(String city:new String[]{"Bhopal","Delhi","Mumbai"}) {
            var cinemas=db.queryForList("SELECT id FROM cinemas WHERE name='Vista Demo Cinema' AND city=? ORDER BY id LIMIT 1",Long.class,city);
            long cinema=cinemas.isEmpty()?db.queryForObject("INSERT INTO cinemas(name,city,address) VALUES ('Vista Demo Cinema',?,'Fictional demonstration venue') RETURNING id",Long.class,city):cinemas.get(0);
            for(int i=0;i<3;i++) {
                String title=new String[]{"The Last Platform (Demo)","Across the Sky (Demo)","A Sunday Match (Demo)"}[i];
                var movies=db.queryForList("SELECT id FROM movies WHERE title=? ORDER BY id LIMIT 1",Long.class,title);
                long movie=movies.isEmpty()?db.queryForObject("INSERT INTO movies(title,language,duration_minutes) VALUES (?,'Hindi',120) RETURNING id",Long.class,title):movies.get(0);
                LocalDate date=LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(i+1);
                String key="vista-movie-"+city+"-"+i+"-"+date;
                if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM movie_shows WHERE demo_key=?)",Boolean.class,key)))continue;
                long show=db.queryForObject("INSERT INTO movie_shows(movie_id,cinema_id,starts_at,demo_key) VALUES (?,?,?,?) RETURNING id",Long.class,movie,cinema,date.atTime(18,0).atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime(),key);
                for(char row='A';row<='D';row++)for(int seat=1;seat<=10;seat++)db.update("INSERT INTO movie_show_seats(show_id,label,price) VALUES (?,?,?)",show,""+row+seat,row=='D'?350:250);
            }
        }
    }
}
