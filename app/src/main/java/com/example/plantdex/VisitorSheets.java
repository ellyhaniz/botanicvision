package com.example.plantdex;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

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

    public static void showShareDiscovery(Activity activity, String name, String rarity, int unlocked, int total) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.sheet_share_discovery, null);
        dialog.setContentView(view);

        TextView tvPreview = view.findViewById(R.id.tvSharePreview);
        tvPreview.setText("\"I just unlocked the " + name + " (" + rarity + ") in PlantDex at the Singapore Botanic Gardens! "
                + unlocked + "/" + total + " discovered.\"");

        View.OnClickListener shareClick = v ->
                Toast.makeText(activity, "Sharing — prototype only.", Toast.LENGTH_SHORT).show();
        view.findViewById(R.id.shareInstagram).setOnClickListener(shareClick);
        view.findViewById(R.id.shareWhatsapp).setOnClickListener(shareClick);
        view.findViewById(R.id.shareFacebook).setOnClickListener(shareClick);
        view.findViewById(R.id.shareMore).setOnClickListener(shareClick);

        view.findViewById(R.id.btnCancelShare).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public static void showFilterInventory(Activity activity) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.sheet_filter_inventory, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btnClearFilter).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            Toast.makeText(activity, "Filter applied.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}
