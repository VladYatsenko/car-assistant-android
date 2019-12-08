package com.carassistant.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.carassistant.R;
import com.carassistant.model.entity.SignEntity;

import java.util.ArrayList;

//@ScreenScope
public class SignAdapter extends RecyclerView.Adapter<SignAdapter.SignViewHolder> {

    private Context context;
    private ArrayList<SignEntity> signs;
//    @Inject
    public SignAdapter(Context context) {
        this.context = context;
        this.signs = new ArrayList<>();
    }

    public ArrayList<SignEntity> getSigns(){
        if (signs != null)
            return signs;
        else return new ArrayList<>();
    }

    public void setSigns(ArrayList<SignEntity> signs) {
        if (signs != null)
            this.signs.addAll(signs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SignViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sign, parent, false);
        return new SignViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SignViewHolder holder, int position) {
        SignEntity sing = signs.get(position);
        holder.bind(sing);
    }

    @Override
    public int getItemCount() {
        if (signs == null) return 0;
        else return signs.size();
    }

    public void setSign(SignEntity sign) {
        if (sign != null) {
            signs.add(0, sign);
            if (signs.size() > 9){
                signs.remove(signs.size() - 1);
            }
            notifyDataSetChanged();
        }
    }

    class SignViewHolder extends RecyclerView.ViewHolder {

        AppCompatImageView image;
        TextView signName, signConfidence;

        SignViewHolder(View v) {
            super(v);
            image = v.findViewById(R.id.signImg);
            signName = v.findViewById(R.id.signName);
            signConfidence = v.findViewById(R.id.signConfidence);
        }

        void bind(SignEntity sing) {
            Glide.with(context)
                    .load(sing.getImage())
                    .into(image);
            signName.setText(sing.getName());

            String confidence = String.format("%.2f", sing.getConfidenceDetection()*100).replace(',', '.');
            signConfidence.setText("Confidence: "+confidence+"%");
        }

    }

}
