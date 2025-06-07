package nie.translator.rtranslator.onboarding;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import nie.translator.rtranslator.R;

class LanguageVH extends RecyclerView.ViewHolder {

    TextView tvName;
    TextView tvCode;
    ImageView ivFlag;

    ImageView selectedIv;
    LanguageVH(View itemView) {
        super(itemView);
        ivFlag = itemView.findViewById(R.id.iv_flag);
        tvName = itemView.findViewById(R.id.tv_name);
        tvCode = itemView.findViewById(R.id.tv_code);
        selectedIv = itemView.findViewById(R.id.select_icon_iv);
    }
}
