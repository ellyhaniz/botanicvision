package com.example.plantdex.visitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.plantdex.R;
import com.example.plantdex.common.entity.Collectible;
import com.example.plantdex.visitor.control.CollectibleController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bottom sheets used across the Visitor identify / collection flow. */
public final class VisitorSheets {

    private VisitorSheets() {}

    public static void showSearchMoreInfo(Activity activity, String query) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.sheet_search_more_info, null);
        dialog.setContentView(view);

        TextView tvQuery = view.findViewById(R.id.tvSearchQuery);
        if (query != null && !query.isEmpty()) {
            tvQuery.setText(query);
        }

        view.findViewById(R.id.btnCancelSearch).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnOpenBrowser).setOnClickListener(v -> {
            try {
                String q = Uri.encode(tvQuery.getText().toString());
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + q)));
                dialog.dismiss();
            } catch (Exception e) {
                view.findViewById(R.id.layoutNoInternet).setVisibility(View.VISIBLE);
            }
        });

        dialog.show();
    }

    public static void showShareDiscovery(Activity activity, String name, String rarity) {
        showShareDiscovery(activity, name, rarity, null);
    }

    /**
     * Shares a discovery: draws a picture with the photo, name, rarity, message and progress
     * (ShareCardRenderer) and opens Android's share menu with it, so the visitor picks
     * Instagram, WhatsApp, Telegram, etc. The message is also sent as text for apps that use it.
     *
     * @param image the visitor's own photo (content://) or the card's picture (http), or null
     */
    public static void showShareDiscovery(Activity activity, String name, String rarity, String image) {
        Toast.makeText(activity, "Preparing your post…", Toast.LENGTH_SHORT).show();
        String message = "I just unlocked the " + name + " in PlantDex at the Singapore Botanic Gardens!";
        // Progress ("2 / 14 discovered") needs the catalog; share without it if that fails.
        CollectibleController.loadAll(activity, new CollectibleController.Callback() {
            @Override
            public void onSuccess(List<Collectible> all) {
                int unlocked = 0;
                for (Collectible c : all) {
                    if (DiscoveryStore.isDiscovered(activity, c.getId())) unlocked++;
                }
                share(activity, name, rarity, image, message, unlocked + " / " + all.size() + " discovered");
            }

            @Override
            public void onError(String error) {
                share(activity, name, rarity, image, message, null);
            }
        });
    }

    private static void share(Activity activity, String name, String rarity, String image,
                              String message, String progress) {
        new Thread(() -> {
            Uri picture = null;
            try {
                Bitmap card = ShareCardRenderer.render(activity, image, name, rarity, message, progress);
                File dir = new File(activity.getCacheDir(), "shared");
                if (dir.isDirectory() || dir.mkdirs()) {
                    File file = new File(dir, "plantdex-discovery.jpg");
                    try (OutputStream out = new FileOutputStream(file)) {
                        card.compress(Bitmap.CompressFormat.JPEG, 92, out);
                    }
                    picture = FileProvider.getUriForFile(
                            activity, activity.getPackageName() + ".fileprovider", file);
                }
            } catch (IOException | RuntimeException e) {
                android.util.Log.w("ShareDiscovery", "Could not make the share picture", e);
            }
            Uri finalPicture = picture;
            String text = message + (progress != null ? " " + progress + "." : "");
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                Intent send = new Intent(Intent.ACTION_SEND);
                send.putExtra(Intent.EXTRA_TEXT, text);
                if (finalPicture != null) {
                    send.setType("image/jpeg");
                    send.putExtra(Intent.EXTRA_STREAM, finalPicture);
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    // Lets the share menu show a preview of the picture.
                    send.setClipData(android.content.ClipData.newRawUri("", finalPicture));
                } else {
                    send.setType("text/plain");
                }
                activity.startActivity(Intent.createChooser(send, "Share your discovery"));
            });
        }).start();
    }

    public interface FilterCallback {
        void onApply(InventoryFilter filter);
    }

    /** #VS-10 — type, rarity and discovery status all actually filter; Clear resets every row. */
    public static void showFilterInventory(Activity activity, InventoryFilter current, FilterCallback callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.sheet_filter_inventory, null);
        dialog.setContentView(view);

        Set<String> types = new HashSet<>(current.types);
        Set<String> rarities = new HashSet<>(current.rarities);
        String[] status = {current.discoveryStatus};

        TextView chipFlora = view.findViewById(R.id.filterTypeFlora);
        TextView chipArchitecture = view.findViewById(R.id.filterTypeArchitecture);
        TextView chipCommon = view.findViewById(R.id.filterRarityCommon);
        TextView chipRare = view.findViewById(R.id.filterRarityRare);
        TextView chipLegendary = view.findViewById(R.id.filterRarityLegendary);
        TextView chipCollected = view.findViewById(R.id.filterStatusCollected);
        TextView chipUndiscovered = view.findViewById(R.id.filterStatusUndiscovered);
        TextView chipAll = view.findViewById(R.id.filterStatusAll);

        // Multi-select group: clicking a chip toggles it on/off independently of the others.
        setUpToggleChip(chipFlora, () -> types.contains("Flora"), sel -> toggle(types, "Flora", sel));
        setUpToggleChip(chipArchitecture, () -> types.contains("Architecture"), sel -> toggle(types, "Architecture", sel));
        setUpToggleChip(chipCommon, () -> rarities.contains("Common"), sel -> toggle(rarities, "Common", sel));
        setUpToggleChip(chipRare, () -> rarities.contains("Rare"), sel -> toggle(rarities, "Rare", sel));
        setUpToggleChip(chipLegendary, () -> rarities.contains("Legendary"), sel -> toggle(rarities, "Legendary", sel));

        // Single-select group: picking one clears the others (like a radio button).
        TextView[] statusChips = {chipCollected, chipUndiscovered, chipAll};
        String[] statusValues = {InventoryFilter.STATUS_COLLECTED, InventoryFilter.STATUS_UNDISCOVERED, InventoryFilter.STATUS_ALL};
        Runnable[] refreshStatus = new Runnable[1];
        refreshStatus[0] = () -> {
            for (int i = 0; i < statusChips.length; i++) {
                applyChipStyle(statusChips[i], statusValues[i].equals(status[0]));
            }
        };
        for (int i = 0; i < statusChips.length; i++) {
            String value = statusValues[i];
            statusChips[i].setOnClickListener(v -> {
                status[0] = value;
                refreshStatus[0].run();
            });
        }
        refreshStatus[0].run();

        view.findViewById(R.id.btnClearFilter).setOnClickListener(v -> {
            callback.onApply(new InventoryFilter());
            dialog.dismiss();
        });
        view.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            callback.onApply(new InventoryFilter(types, rarities, status[0]));
            dialog.dismiss();
        });

        dialog.show();
    }

    private interface ChipState {
        boolean isSelected();
    }

    private interface ChipToggle {
        void onToggled(boolean nowSelected);
    }

    private static void setUpToggleChip(TextView chip, ChipState state, ChipToggle onToggle) {
        applyChipStyle(chip, state.isSelected());
        chip.setOnClickListener(v -> {
            boolean nowSelected = !state.isSelected();
            onToggle.onToggled(nowSelected);
            applyChipStyle(chip, nowSelected);
        });
    }

    private static void toggle(Set<String> set, String value, boolean selected) {
        if (selected) {
            set.add(value);
        } else {
            set.remove(value);
        }
    }

    private static void applyChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(chip.getContext().getColor(selected ? R.color.white : R.color.plantdex_text_muted));
    }
}
