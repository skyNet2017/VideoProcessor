package com.hss01248.videocompress.listener;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.hss01248.videocompress.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class CompressProgressListAdapter extends BaseAdapter {

    private final Context context;
    private final List<CompressProgressItem> displayItems = new ArrayList<>();
    private final SimpleDateFormat enqueueTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    CompressProgressListAdapter(Context context) {
        this.context = context;
    }

    void setItems(List<CompressProgressItem> source) {
        displayItems.clear();
        if (source != null) {
            displayItems.addAll(source);
            Collections.sort(displayItems, new Comparator<CompressProgressItem>() {
                @Override
                public int compare(CompressProgressItem a, CompressProgressItem b) {
                    return Long.compare(b.joinOrder, a.joinOrder);
                }
            });
        }
        notifyDataSetChanged();
    }

    CompressProgressItem getItemAt(int position) {
        if (position < 0 || position >= displayItems.size()) {
            return null;
        }
        return displayItems.get(position);
    }

    @Override
    public int getCount() {
        return displayItems.size();
    }

    @Override
    public Object getItem(int position) {
        return displayItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return displayItems.get(position).joinOrder;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_compress_progress, parent, false);
            holder = new ViewHolder();
            holder.ivThumbnail = convertView.findViewById(R.id.iv_thumbnail);
            holder.tvFileName = convertView.findViewById(R.id.tv_file_name);
            holder.tvEnqueueTime = convertView.findViewById(R.id.tv_enqueue_time);
            holder.tvStatus = convertView.findViewById(R.id.tv_status);
            holder.pbProgress = convertView.findViewById(R.id.pb_progress);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CompressProgressItem item = displayItems.get(position);
        Glide.with(context).clear(holder.ivThumbnail);
        Glide.with(context)
                .asBitmap()
                .load(new File(item.inputPath))
                .frame(0)
                .apply(new RequestOptions().transform(new CenterCrop()))
                .into(holder.ivThumbnail);
        holder.tvFileName.setText(new File(item.inputPath).getName());
        holder.tvEnqueueTime.setText(context.getString(
                R.string.vc_compress_enqueue_time,
                enqueueTimeFormat.format(new Date(item.enqueueTimeMillis))));
        holder.tvStatus.setText(formatStatus(item));
        if (item.status == CompressProgressItem.Status.RUNNING) {
            holder.pbProgress.setVisibility(View.VISIBLE);
            holder.pbProgress.setProgress(item.progress);
        } else {
            holder.pbProgress.setVisibility(View.GONE);
        }
        return convertView;
    }

    private String formatStatus(CompressProgressItem item) {
        switch (item.status) {
            case WAITING:
                return context.getString(R.string.vc_compress_status_waiting);
            case RUNNING:
                return context.getString(R.string.vc_compress_status_running, item.progress);
            case SUCCESS:
                return context.getString(R.string.vc_compress_status_success);
            case ERROR:
                if (item.errorMessage != null && !item.errorMessage.isEmpty()) {
                    return context.getString(R.string.vc_compress_status_error, item.errorMessage);
                }
                return context.getString(R.string.vc_compress_status_error_unknown);
            case CANCELLED:
                return context.getString(R.string.vc_compress_status_cancelled);
            default:
                return "";
        }
    }

    private static class ViewHolder {
        ImageView ivThumbnail;
        TextView tvFileName;
        TextView tvEnqueueTime;
        TextView tvStatus;
        ProgressBar pbProgress;
    }
}
