package com.example.imageclassificationdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class Treatments extends AppCompatActivity {
    TextView textView_treatment,textView_treatment2,textView_disease,textView_disease2,textView_usage,textView_usage2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treatments);

        textView_disease=findViewById(R.id.diseaseId);
        textView_disease2=findViewById(R.id.diseaseId2);

        textView_treatment=findViewById(R.id.treatmentId);
        textView_treatment2=findViewById(R.id.treatmentId2);

        textView_usage=findViewById(R.id.usaId);
        textView_usage2=findViewById(R.id.usaId2);


        Bundle t=getIntent().getExtras();
        String test=t.getString("the disease");

        if(test.equals("Acne")){
            textView_disease.setText("Acne");
            textView_disease2.setText("Acne");

            textView_treatment.setText("Acne Cream");
            textView_treatment2.setText("Agera Cream");

            textView_usage.setText("2 times per a day");
            textView_usage2.setText("3 times per a day");

        }
        if(test.equals("Vitiligo")){
            textView_disease.setText("Vitiligo");
            textView_disease2.setText("Vitiligo");

            textView_treatment.setText(" Vitix Gel");
            textView_treatment2.setText("  Vitise care");

            textView_usage.setText("3 times per a day");
            textView_usage2.setText("2 times per a day");

        }

        if(test.equals("Rosacea")){
            textView_disease.setText("Rosacea");
            textView_disease2.setText("Rosacea");

            textView_treatment.setText(" forces of nature");
            textView_treatment2.setText(" cerave  Cream");

            textView_usage.setText("3 times per a day");
            textView_usage2.setText("2 times per a day");

        }
        if(test.equals("Psoriasis")){
            textView_disease.setText("Psoriasis");
            textView_disease2.setText("Psoriasis");

            textView_treatment.setText("Jojoba Oil");
            textView_treatment2.setText("Hardcover");

            textView_usage.setText("3 times per a day");
            textView_usage2.setText("2 times per a day");

        }
        if(test.equals("measles")){
            textView_disease.setText("measles");
            textView_disease2.setText("measles");

            textView_treatment.setText("antibiotic trimethoprim");
            textView_treatment2.setText("measles Hardcover ");

            textView_usage.setText("3 times per a day");
            textView_usage2.setText("2 times per a day");

        }








    }// end onCreate method
}// treamtents class