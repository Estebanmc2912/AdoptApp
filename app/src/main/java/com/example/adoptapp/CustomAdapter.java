package com.example.adoptapp;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class CustomAdapter implements ListAdapter {

    private ArrayList<Animal> arrayList;
    private Context context;

    CustomAdapter(Context context, ArrayList<Animal> arrayList) {
        this.arrayList=arrayList;
        this.context=context;
    }
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }
    @Override
    public boolean isEnabled(int position) {
        return true;
    }
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }
    @Override
    public int getCount() {
        return arrayList.size();
    }
    @Override
    public Object getItem(int position) {
        return position;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Animal animal=arrayList.get(position);

        if(convertView==null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView=layoutInflater.inflate(R.layout.layout_item_animal, null);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(view.getContext(), animal.getId(), Toast.LENGTH_SHORT).show();
                }
            });

            TextView textViewNombre =convertView.findViewById(R.id.textViewItemNombreAnimal);
            textViewNombre.setText(animal.getNombre());
            String datos = animal.getTamano()+"\n"+animal.getEdad()+"\nEn "+animal.getCiudad()
                    +"\nA "+animal.getDistancia()+" km de tu ubicación actual"
                    +"\nResponsable: "+animal.getNombreResponsable();
            TextView textViewDatos =convertView.findViewById(R.id.textViewItemDetallesAnimal);
            textViewDatos.setText(datos);
            ImageView imageViewFoto = convertView.findViewById(R.id.imageViewItemAnimal);

            if(!animal.getUrlFotoPrincipal().equals("") ) {
                try {
                    //String imageUrl = "https://firebasestorage.googleapis.com/v0/b/adoptapp-77514.appspot.com/o/animales%2Fexx9WvDpQZM11MI3Cubi%2Flulu1.jpg?alt=media&token=9715c5a1-fed5-42de-98c4-7d42fe771cce";
                    String imageUrl = animal.getUrlFotoPrincipal();
                    InputStream URLcontent = (InputStream) new URL(imageUrl).getContent();
                    Drawable image = Drawable.createFromStream(URLcontent, "your source link");
                    imageViewFoto.setImageDrawable(image);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
    @Override
    public int getViewTypeCount() {
        return arrayList.size();
    }
    @Override
    public boolean isEmpty() {
        return false;
    }
}
