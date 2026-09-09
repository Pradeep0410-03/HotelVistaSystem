package com.hotelvista;

import com.hotelvista.demo.DemoCatalogue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class DemoCatalogueIT {
    @Autowired JdbcTemplate jdbc;
    @Test void catalogueIsCompleteAndRerunPreservesInventory() throws Exception {
        var loader=new DemoCatalogue(jdbc);
        loader.run(new DefaultApplicationArguments(new String[0]));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM properties WHERE demo_key LIKE 'hotelvista-demo-v1-%'",Integer.class)).isEqualTo(750);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT city) FROM properties WHERE demo_key IS NOT NULL",Integer.class)).isEqualTo(30);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM room_types r JOIN properties p ON r.property_id=p.id WHERE p.demo_key IS NOT NULL",Integer.class)).isEqualTo(1950);
        long id=jdbc.queryForObject("SELECT min(r.id) FROM room_types r JOIN properties p ON p.id=r.property_id WHERE p.demo_key IS NOT NULL",Long.class);
        jdbc.update("UPDATE room_inventory SET reserved_quantity=1 WHERE room_type_id=?",id);
        loader.run(new DefaultApplicationArguments(new String[0]));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM properties WHERE demo_key IS NOT NULL",Integer.class)).isEqualTo(750);
        assertThat(jdbc.queryForObject("SELECT min(reserved_quantity) FROM room_inventory WHERE room_type_id=?",Integer.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM room_inventory i JOIN room_types r ON r.id=i.room_type_id JOIN properties p ON p.id=r.property_id WHERE p.demo_key IS NOT NULL",Integer.class)).isEqualTo(175500);
    }
}
