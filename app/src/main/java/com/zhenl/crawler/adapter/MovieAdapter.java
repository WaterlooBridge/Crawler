package com.zhenl.crawler.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.zhenl.crawler.R;
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.base.RecyclerAdapter;

import java.util.List;

/**
 * Created by lin on 2018/6/9.
 */
public class MovieAdapter extends RecyclerAdapter {

    private List<MovieModel> list;

    public MovieAdapter(List<MovieModel> list) {
        this.list = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new Holder(view);
    }

    @Override
    public int getContentItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Holder holder = (Holder) viewHolder;
        MovieModel model = list.get(position);
        Glide.with(holder.iv.getContext()).load(model.getImg()).centerCrop().into(holder.iv);
        holder.tvTitle.setText(model.title);
        holder.tvDate.setText(model.date);
    }

    public void refresh(List<MovieModel> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    private class Holder extends RecyclerHolder {

        private ImageView iv;
        private TextView tvTitle;
        private TextView tvDate;

        public Holder(View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.iv);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}
