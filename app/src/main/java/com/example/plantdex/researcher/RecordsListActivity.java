package com.example.plantdex.researcher;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;

/** Lists the records a Researcher has logged. Rows are demo data for now,
 *  each opening RecordDetailActivity with matching demo fields. */
public class RecordsListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records_list);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show());

        findViewById(R.id.itemTembusu).setOnClickListener(v -> startActivity(
                RecordDetailActivity.botanical(this,
                        "Tembusu Tree", "Record #0231", "15 Sep 2026, 10:22:41",
                        "Cyrtophyllum fragrans", "Gentianaceae",
                        "Dark fissured bark, small cream flowers, spreading crown.",
                        "Appears on the reverse of the Singapore five-dollar note.")));

        findViewById(R.id.itemRaintree).setOnClickListener(v -> startActivity(
                RecordDetailActivity.botanical(this,
                        "Raintree", "Record #0229", "10 Sep 2026, 09:04:12",
                        "Samanea saman", "Fabaceae",
                        "Broad umbrella-shaped canopy, bipinnate leaves that fold up at dusk.",
                        "Named for the way its canopy appears to “rain” moisture on cloudy afternoons.")));

        findViewById(R.id.itemAngsana).setOnClickListener(v -> startActivity(
                RecordDetailActivity.botanical(this,
                        "Angsana Tree", "Record #0217", "02 Sep 2026, 16:47:30",
                        "Pterocarpus indicus", "Fabaceae",
                        "Fast-growing shade tree with bright yellow flowers and winged seed pods.",
                        "One of the most widely planted roadside trees in Singapore.")));

        findViewById(R.id.itemBandstand).setOnClickListener(v -> startActivity(
                RecordDetailActivity.architectural(this,
                        "Bandstand", "Record #0198", "12 Sep 2026, 14:18:05",
                        "Bandstand Hill, Tanglin Core", "Victorian", "1930",
                        "An octagonal cast iron bandstand set on a low rise, originally used for evening band performances.")));

        findViewById(R.id.itemClockTower).setOnClickListener(v -> startActivity(
                RecordDetailActivity.architectural(this,
                        "Old Clock Tower", "Record #0184", "06 Sep 2026, 11:52:19",
                        "Civic District", "Neo-classical", "1908",
                        "A four-faced clock tower marking the entrance to the old civic quarter.")));

        findViewById(R.id.itemFountainGate).setOnClickListener(v -> startActivity(
                RecordDetailActivity.architectural(this,
                        "Fountain Gate", "Record #0176", "29 Aug 2026, 08:36:54",
                        "Botanic Gardens East Entrance", "Edwardian", "1921",
                        "An ornamental cast-iron gate framing a small fountain courtyard.")));
    }
}
