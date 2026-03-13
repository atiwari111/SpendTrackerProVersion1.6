package com.spendtracker.pro;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import java.io.InputStream;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import java.util.*;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
    private List<Transaction> list = new ArrayList<>();
    private boolean editEnabled = false;

    public TransactionAdapter() {}
    public TransactionAdapter(boolean editEnabled) { this.editEnabled = editEnabled; }

    public void setTransactions(List<Transaction> l) {
        this.list = l != null ? l : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_transaction, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        Transaction t = list.get(i);
        String merchant = t.merchant != null ? t.merchant : "Unknown";

        // ── Merchant logo: try PNG first, fall back to colored initial ──
        String logoFile = MerchantLogoProvider.getLogo(merchant);
        if (logoFile != null) {
            // Real PNG mapped — try to load from assets
            try {
                InputStream is = h.itemView.getContext().getAssets().open(logoFile);
                Drawable d = Drawable.createFromStream(is, null);
                is.close();
                if (h.ivLogo != null) {
                    h.ivLogo.setVisibility(View.VISIBLE);
                    h.ivLogo.setImageDrawable(d);
                    h.tvIcon.setVisibility(View.GONE);
                } else {
                    setInitialFallback(h, merchant, t);
                }
            } catch (Exception e) {
                // PNG not bundled yet — use colored initial
                setInitialFallback(h, merchant, t);
            }
        } else {
            setInitialFallback(h, merchant, t);
        }

        h.tvMerchant.setText(merchant);

        // ── Category / amount ─────────────────────────────────────
        if (t.isSelfTransfer) {
            h.tvCategory.setText("🔄 Self-Transfer");
            h.tvCategory.setTextColor(Color.parseColor("#94A3B8"));
            h.tvAmount.setText("—");
            h.tvAmount.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            h.tvCategory.setText(t.category != null ? t.category : "Others");
            h.tvCategory.setTextColor(Color.parseColor("#A78BFA"));
            h.tvAmount.setText("−" + t.getFormattedAmount());
            h.tvAmount.setTextColor(Color.parseColor("#EF4444"));
        }

        h.tvDate.setText(t.getFormattedDateTime());
        h.tvPayment.setText(t.paymentDetail != null ? t.paymentDetail : t.paymentMethod);

        h.itemView.setOnClickListener(v -> { if (editEnabled) openEdit(h.itemView.getContext(), t); });
        h.itemView.setOnLongClickListener(v -> { openEdit(h.itemView.getContext(), t); return true; });
    }

    private void setInitialFallback(VH h, String merchant, Transaction t) {
        Drawable initial = MerchantLogoProvider.getInitialDrawable(merchant);
        if (h.ivLogo != null) {
            h.ivLogo.setVisibility(View.VISIBLE);
            h.ivLogo.setImageDrawable(initial);
            h.tvIcon.setVisibility(View.GONE);
        } else {
            // Layout doesn't have ivLogo yet — fall back to emoji in tvIcon
            if (h.tvIcon != null) {
                h.tvIcon.setVisibility(View.VISIBLE);
                h.tvIcon.setText(t.categoryIcon != null ? t.categoryIcon : "💼");
            }
        }
    }

    private void openEdit(Context ctx, Transaction t) {
        Intent intent = new Intent(ctx, AddExpenseActivity.class);
        intent.putExtra(AddExpenseActivity.EXTRA_TRANSACTION_ID, t.id);
        ctx.startActivity(intent);
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvMerchant, tvCategory, tvDate, tvAmount, tvPayment;
        ImageView ivLogo; // optional — present in layout if added

        VH(View v) {
            super(v);
            tvIcon     = v.findViewById(R.id.tvIcon);
            tvMerchant = v.findViewById(R.id.tvMerchant);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvDate     = v.findViewById(R.id.tvDate);
            tvAmount   = v.findViewById(R.id.tvAmount);
            tvPayment  = v.findViewById(R.id.tvPayment);
            ivLogo     = v.findViewById(R.id.ivLogo); // null-safe if not in layout
        }
    }
}
