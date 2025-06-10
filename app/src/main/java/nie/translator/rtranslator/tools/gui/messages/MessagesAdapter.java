/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslator.tools.gui.messages;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;

import java.util.ArrayList;

import nie.translator.rtranslator.Global;
import nie.translator.rtranslator.R;
import nie.translator.rtranslator.standby.StandbyManager;
import opencc.OpenCC;

/**
 * Is used to connect to the RecycleView, which functions as a ListView, a list of strings, which will be inserted in the ViewHolder layout and this will be inserted in the list
 **/
public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int MINE = 0;
    private static final int NON_MINE = 1;
    //private static final int PREVIEW = 2;
    private ArrayList<GuiMessage> mResults = new ArrayList<>();
    private Callback callback;
    OpenCC openCC = new OpenCC();
    public MessagesAdapter(ArrayList<GuiMessage> messages, @NonNull Callback callback) {
        this.callback = callback;
        LogUtils.d("MessagesAdapter", "MessagesAdapter");
        if (messages != null) {
            if (messages.size() > 0) {
                callback.onFirstItemAdded();
            }
            mResults.addAll(messages);
            notifyItemRangeInserted(0, messages.size() - 1);
        }
        openCC.setConversion("t2s");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case MINE:
                return new SendHolder(LayoutInflater.from(parent.getContext()), parent);
            case NON_MINE:
                return new ReceivedHolder(LayoutInflater.from(parent.getContext()), parent);
        }
        return new SendHolder(LayoutInflater.from(parent.getContext()), parent);  // to not return null
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LogUtils.d("MessagesAdapter", "onBindViewHolder");
        if (holder instanceof MessageHolder) {
            GuiMessage message = mResults.get(position);
            if (holder instanceof ReceivedHolder && message.getMessage().getSender() != null) {
                ((ReceivedHolder) holder).contentLayout.setVisibility(View.GONE);
                ((ReceivedHolder) holder).containerSender.setVisibility(View.VISIBLE);
                ((ReceivedHolder) holder).sender.setText(message.getMessage().getSender().getName());
                Log.d("recyclerview", "RecyclerView bind sender");
            }
            ((MessageHolder) holder).setText(openCC.convert(message.getMessage().getText()));
            ((MessageHolder) holder).setSrcText(openCC.convert(message.getMessage().getSrcText()));
            Log.d("recyclerview", "RecyclerView bind text");
            //holder.itemView.requestLayout();
        }
    }

    @Override
    public int getItemViewType(int position) {
        GuiMessage message = mResults.get(position);
        if (message.isMine()) {
            return MINE;
        } else {
            return NON_MINE;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mResults.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull RecyclerView.ViewHolder holder) {
        return super.onFailedToRecycleView(holder);
    }

    public void addMessage(GuiMessage message) {
        LogUtils.d("MessagesAdapter", "addMessage");
        if (getItemCount() == 0) {
            callback.onFirstItemAdded();
        }
        mResults.add(message);
        notifyItemInserted(getItemCount() - 1);
    }

    public void setMessage(int index, GuiMessage message) {
        LogUtils.d("MessagesAdapter", "setMessage");
        mResults.set(index, message);
        //notifyItemRangeChanged(0, getItemCount());
        notifyItemChanged(index);
    }

    public int getMessageIndex(long messageID) {
        for (int i = 0; i < mResults.size(); i++) {
            if (mResults.get(i).getMessageID() == messageID) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public GuiMessage getMessage(long messageID) {
        for (int i = 0; i < mResults.size(); i++) {
            if (mResults.get(i).getMessageID() == messageID) {
                return mResults.get(i);
            }
        }
        return null;
    }

    public GuiMessage getMessage(int index) {
        return mResults.get(index);
    }

    public int indexOf(GuiMessage message) {
        return mResults.indexOf(message);
    }

    public ArrayList<GuiMessage> getMessages() {
        return mResults;
    }

    public void clear() {
        //清空内容，并刷新界面
        mResults.clear();
        notifyDataSetChanged();
    }

    /**
     * The layout for each item in the RecicleView list
     */
    private static class ReceivedHolder extends RecyclerView.ViewHolder implements MessageHolder {
        TextView text, srcText;
        LinearLayout containerSender,contentLayout;
        TextView textSender;
        TextView sender;

        ReceivedHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.component_message_received, parent, false));
            text = itemView.findViewById(R.id.text_content);
            srcText = itemView.findViewById(R.id.text_content_src);
            containerSender = itemView.findViewById(R.id.sender_container);
            contentLayout = itemView.findViewById(R.id.text_content_layout);
            textSender = itemView.findViewById(R.id.text_content2);
            sender = itemView.findViewById(R.id.text_sender);
        }

        @Override
        public void setText(String text) {
            this.textSender.setText(text);
            this.text.setText(text);
        }

        @Override
        public void setSrcText(String text) {
            this.srcText.setText(text);
        }
    }

    /**
     * The layout for each item in the RecicleView list
     */
    private static class SendHolder extends RecyclerView.ViewHolder implements MessageHolder {
        TextView text, srcText;
        CardView card;

        SendHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.component_message_send, parent, false));
            text = itemView.findViewById(R.id.text);
            srcText = itemView.findViewById(R.id.text_src);
            card = itemView.findViewById(R.id.card);
        }

        @Override
        public void setText(String text) {
            this.text.setText(text);
        }

        @Override
        public void setSrcText(String text) {
            this.srcText.setText(text);
        }
    }

    interface MessageHolder {
        void setText(String text);

        void setSrcText(String text);
    }

    public interface Callback {
        void onFirstItemAdded();
    }
}
