package com.example.plantdex.common.boundary;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.Session;
import com.example.plantdex.common.entity.Collectible;
import com.example.plantdex.visitor.CollectibleCardActivity;
import com.example.plantdex.visitor.CollectibleInventoryActivity;
import com.example.plantdex.visitor.DiscoveryStore;
import com.example.plantdex.visitor.control.CollectibleController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Notifications, built from what the app already knows (there is no push service yet):
 * the account being created, each collectible the visitor unlocked, and new plants or
 * landmarks added to the collection that they haven't found yet. Newest first.
 */
public class NotificationsActivity extends AppCompatActivity {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter UNLOCKED = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    /** One row. {@code open} is what tapping it does (may be null). */
    private static final class Item {
        final LocalDateTime when; final boolean hasTime;
        final int icon; final String title, body; final Intent open;

        Item(LocalDateTime when, boolean hasTime, int icon, String title, String body, Intent open) {
            this.when = when; this.hasTime = hasTime; this.icon = icon;
            this.title = title; this.body = body; this.open = open;
        }
    }

    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        role = Session.getRole(this);
        if (role == null) role = Roles.VISITOR;
        BottomNavHelper.setup(this, findViewById(R.id.navContainer), role);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Roles.VISITOR.equals(role)) {
            show(new ArrayList<>(), null);
            return;
        }
        CollectibleController.loadAll(this, new CollectibleController.Callback() {
            @Override
            public void onSuccess(List<Collectible> collectibles) {
                show(collectibles, null);
            }

            @Override
            public void onError(String message) {
                show(new ArrayList<>(), "Couldn't load your discoveries. " + message);
            }
        });
    }

    private void show(List<Collectible> catalog, String problem) {
        List<Item> items = new ArrayList<>();

        LocalDate joined = parseDay(Session.getMemberSince(this));
        if (joined != null) {
            items.add(new Item(joined.atStartOfDay(), false, R.drawable.ic_person_outline,
                    "Welcome to PlantDex!",
                    Roles.VISITOR.equals(role)
                            ? "Your account was created. Scan plants and landmarks in the gardens to start your collection."
                            : "Your account was created.",
                    null));
        }

        // New plants / landmarks still to find, grouped by the day they were added.
        Map<LocalDate, int[]> added = new TreeMap<>();   // day -> {plants, landmarks}
        for (Collectible c : catalog) {
            boolean architecture = "Architecture".equals(c.getCategory());
            String unlocked = DiscoveryStore.getUnlockedAt(this, c.getId());
            LocalDateTime when = parseUnlocked(unlocked);
            if (when != null) {
                items.add(new Item(when, true, architecture ? R.drawable.ic_building : R.drawable.ic_leaf,
                        "New discovery: " + c.getName(),
                        "You unlocked a " + c.getRarity() + " " + (architecture ? "landmark" : "plant")
                                + " card. Tap to view it.",
                        CollectibleCardActivity.discovered(this, c)));
                continue;
            }
            LocalDateTime addedAt = parseIso(c.getAddedAt());
            if (addedAt != null) {
                int[] counts = added.computeIfAbsent(addedAt.toLocalDate(), d -> new int[2]);
                counts[architecture ? 1 : 0]++;
            }
        }
        for (Map.Entry<LocalDate, int[]> day : added.entrySet()) {
            int plants = day.getValue()[0], landmarks = day.getValue()[1];
            String what = join(count(plants, "new plant", "new plants"), count(landmarks, "new landmark", "new landmarks"));
            boolean one = plants + landmarks == 1;
            items.add(new Item(day.getKey().atStartOfDay(), false,
                    landmarks > 0 && plants == 0 ? R.drawable.ic_building : R.drawable.ic_leaf,
                    "New to discover",
                    what + (one ? " was" : " were") + " added to the collection. Can you find "
                            + (one ? "it" : "them") + "?",
                    new Intent(this, CollectibleInventoryActivity.class)));
        }

        items.sort((a, b) -> b.when.compareTo(a.when));

        LinearLayout list = findViewById(R.id.notificationList);
        list.removeAllViews();
        for (Item item : items) list.addView(row(list, item));

        TextView status = findViewById(R.id.tvNotificationsStatus);
        if (problem != null) {
            status.setText(problem);
            status.setVisibility(View.VISIBLE);
        } else if (items.isEmpty()) {
            status.setText("You're all caught up — no notifications yet.");
            status.setVisibility(View.VISIBLE);
        } else {
            status.setVisibility(View.GONE);
        }
    }

    private View row(LinearLayout parent, Item item) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_notification, parent, false);
        ((ImageView) row.findViewById(R.id.notifIcon)).setImageResource(item.icon);
        ((TextView) row.findViewById(R.id.notifTitle)).setText(item.title);
        ((TextView) row.findViewById(R.id.notifBody)).setText(item.body);
        ((TextView) row.findViewById(R.id.notifTime)).setText(when(item));
        if (item.open != null) row.setOnClickListener(v -> startActivity(item.open));
        return row;
    }

    /** "Today, 21:59", "Yesterday", or "05 Nov 2026". */
    private static String when(Item item) {
        LocalDate day = item.when.toLocalDate(), today = LocalDate.now();
        String time = item.hasTime ? ", " + item.when.format(TIME) : "";
        if (day.equals(today)) return "Today" + time;
        if (day.equals(today.minusDays(1))) return "Yesterday" + time;
        return day.format(DAY) + time;
    }

    private static String count(int n, String one, String many) {
        return n == 0 ? "" : n + " " + (n == 1 ? one : many);
    }

    private static String join(String a, String b) {
        if (a.isEmpty()) return capitalise(b);
        return capitalise(b.isEmpty() ? a : a + " and " + b);
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static LocalDate parseDay(String s) {
        try { return s == null ? null : LocalDate.parse(s, DAY); } catch (RuntimeException e) { return null; }
    }

    private static LocalDateTime parseUnlocked(String s) {
        try { return s == null ? null : LocalDateTime.parse(s, UNLOCKED); } catch (RuntimeException e) { return null; }
    }

    private static LocalDateTime parseIso(String s) {
        try {
            return s == null ? null
                    : OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
