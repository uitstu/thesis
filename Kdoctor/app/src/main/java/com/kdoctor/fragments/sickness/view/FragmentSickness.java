package com.kdoctor.fragments.sickness.view;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.kdoctor.R;
import com.kdoctor.api.RestAIServices;
import com.kdoctor.api.RestServices;
import com.kdoctor.dialogs.CategoryDialog;
import com.kdoctor.dialogs.QuestionDialog;
import com.kdoctor.dialogs.SicknessInfoDialog;
import com.kdoctor.dialogs.SicknessQuestionDialog;
import com.kdoctor.dialogs.SicknessesDialog;
import com.kdoctor.dialogs.TypeInfoDialog;
import com.kdoctor.fragments.sickness.adapters.RecyclerViewAdapterCategory;
import com.kdoctor.fragments.sickness.presenter.FragmentSicknessPresenter;
import com.kdoctor.fragments.vaccines.adapters.RecyclerViewAdapterVaccine;
import com.kdoctor.main.view.MainActivity;
import com.kdoctor.models.Question;
import com.kdoctor.models.Sickness;
import com.kdoctor.models.SicknessCategory;
import com.kdoctor.models.Vaccine;
import com.kdoctor.sql.DbManager;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class FragmentSickness extends Fragment implements IFragmentSickness {

    FragmentSicknessPresenter presenter;
    List<SicknessCategory> categories;
    ProgressDialog progressDialog;

    @BindView(R.id.rv_categories)
    RecyclerView rvCategories;
    @BindView(R.id.fab_heart)
    FloatingActionButton fabHeart;
    @BindView(R.id.fab_find)
    FloatingActionButton fabFind;
    @BindView(R.id.fab_investigate)
    FloatingActionButton fabInvestigate;
    @BindView(R.id.fl_label)
    FrameLayout flLabel;
    @BindView(R.id.srl_categories)
    SwipeRefreshLayout srlCategories;
    @BindView(R.id.srl_sickness)
    SwipeRefreshLayout srlSickness;
    @BindView(R.id.fam)
    FloatingActionsMenu fam;

    @BindView(R.id.rv_sickness)
    RecyclerView rvSickness;

    public SicknessesAdapter getSicknessesAdapter() {
        return sicknessesAdapter;
    }

    SicknessesAdapter sicknessesAdapter;
    SicknessCategory category;
    OnClickListener onClickListener;

    RecyclerViewAdapterCategory adapter;

    String code;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sickness, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        presenter = new FragmentSicknessPresenter(this);

        rvSickness.setLayoutManager(new LinearLayoutManager(getActivity()));
        List<Sickness> sicknesses = new ArrayList<>();
        sicknessesAdapter = new SicknessesAdapter(sicknesses, new OnClickListener() {
            @Override
            public void onResearchClickListener(Sickness sickness) {
                SicknessInfoDialog dialog = new SicknessInfoDialog(sickness, new SicknessInfoDialog.OnClickListener() {
                    @Override
                    public void onSelectListener(Sickness sickness, boolean isSelected) {
                        sickness.setSelected(isSelected);
                        DbManager.getInstance(getContext()).selectRecord(DbManager.SICKNESSES, sickness, sickness.getId());
                    }
                });
                dialog.show(getFragmentManager(), "");
            };

            @Override
            public void onNoteClickListener(Sickness sickness, boolean isSelected) {
                sickness.setSelected(isSelected);
                DbManager.getInstance(getContext()).selectRecord(DbManager.SICKNESSES, sickness, sickness.getId());
            }
        });
        rvSickness.setAdapter(sicknessesAdapter);
        rvSickness.setHasFixedSize(false);

        srlCategories.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                srlCategories.setRefreshing(true);
                presenter.getCategories();
            }
        });
        srlSickness.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                srlSickness.setRefreshing(true);
                RestServices.getInstance().getServices().getSicknessCategory(category.getId(), 0, 5, new Callback<List<Sickness>>() {
                    @Override
                    public void success(List<Sickness> sicknesses, Response response) {
                        sicknessesAdapter.sicknesses.clear();
                        sicknessesAdapter.sicknesses.addAll(sicknesses);
                        sicknessesAdapter.notifyDataSetChanged();
                        srlCategories.setVisibility(View.GONE);
                        srlSickness.setVisibility(View.VISIBLE);
                        ((MainActivity)getActivity()).go(category.getName());
                        if (srlSickness.isRefreshing()) {
                            srlSickness.setRefreshing(false);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        if (srlSickness.isRefreshing()) {
                            srlSickness.setRefreshing(false);
                        }
                    }
                });
            }
        });

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("Vui lòng đợi...");

        categories = new ArrayList<SicknessCategory>();

        rvCategories.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new RecyclerViewAdapterCategory(categories, new RecyclerViewAdapterCategory.OnItemClickListener() {
            @Override
            public void onItemClickListener(final SicknessCategory category) {
                FragmentSickness.this.category = category;
                RestServices.getInstance().getServices().getSicknessCategory(category.getId(), 0, 5, new Callback<List<Sickness>>() {
                    @Override
                    public void success(List<Sickness> sicknesses, Response response) {
                        sicknessesAdapter.sicknesses.clear();
                        sicknessesAdapter.sicknesses.addAll(sicknesses);
                        sicknessesAdapter.notifyDataSetChanged();
                        srlCategories.setVisibility(View.GONE);
                        srlSickness.setVisibility(View.VISIBLE);
                        ((MainActivity)getActivity()).go(category.getName());
                    }

                    @Override
                    public void failure(RetrofitError error) {

                    }
                });
            }
        });
        rvCategories.setAdapter(adapter);
        rvCategories.setHasFixedSize(false);

        initListeners();

        showLoading();
        presenter.getCategories();

    }

    void initListeners(){
        fam.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                flLabel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onMenuCollapsed() {
                flLabel.setVisibility(View.GONE);
            }
        });

        fabHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Sickness> sicknesses = DbManager.getInstance(getContext()).getRecords(DbManager.SICKNESSES, Sickness.class);
                if (sicknesses != null){
                    for (int i = sicknesses.size() - 1; i >= 0; i--){
                        if (sicknesses.get(i).isSelected() == false){
                            sicknesses.remove(i);
                        }
                    }
                }
                SicknessesDialog dialog = new SicknessesDialog(sicknesses, new SicknessesDialog.OnClickListener() {
                    @Override
                    public void onResearchClickListener(Sickness sickness) {
                        SicknessInfoDialog infoDialog = new SicknessInfoDialog(sickness, new SicknessInfoDialog.OnClickListener() {
                            @Override
                            public void onSelectListener(Sickness sickness, boolean isSelected) {
                                sickness.setSelected(isSelected);
                                DbManager.getInstance(getContext()).selectRecord(DbManager.SICKNESSES, sickness, sickness.getId());
                            }
                        });
                        infoDialog.show(getFragmentManager(), "");
                    }

                    @Override
                    public void onNoteClickListener(Sickness sickness, boolean isSelected) {
                        //DbManager.getInstance(getContext()).selectRecord(DbManager.SICKNESSES, sickness, sickness.getId());
                    }
                });
                dialog.show(getFragmentManager(), "");
            }
        });

        fabInvestigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CategoryDialog dialog = new CategoryDialog(categories, new CategoryDialog.OnAnswerListener() {
                    @Override
                    public void onAnswerListener(SicknessCategory answer) {
                        code = answer.getName() + "*";
                        callQuestion(answer.getUrlAPI(), "S");
                    }
                });
                dialog.show(getFragmentManager(),"");
            }
        });

        fabFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TypeInfoDialog dialog = new TypeInfoDialog("Nhập tên bệnh", new TypeInfoDialog.OnClickListener() {
                    @Override
                    public void onPositiveButtonClickListener(String value) {
                        RestServices.getInstance().getServices().searchSickness(value, new Callback<List<Sickness>>() {
                            @Override
                            public void success(List<Sickness> sicknesses, Response response) {
                                if (sicknesses.size() == 0) {
                                    Toast.makeText(getContext(), "Không có kết quả nào...", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                SicknessesDialog sicknessesDialog = new SicknessesDialog(sicknesses, new SicknessesDialog.OnClickListener() {
                                    @Override
                                    public void onResearchClickListener(Sickness sickness) {
                                        SicknessInfoDialog infoDialog = new SicknessInfoDialog(sickness, new SicknessInfoDialog.OnClickListener() {
                                            @Override
                                            public void onSelectListener(Sickness sickness, boolean isSelected) {
                                                sickness.setSelected(true);
                                                DbManager.getInstance(getContext()).selectRecord(DbManager.SICKNESSES, sickness, sickness.getId());
                                            }
                                        });
                                        infoDialog.show(getFragmentManager(), "");
                                    }

                                    @Override
                                    public void onNoteClickListener(Sickness sickness, boolean isSelected) {
                                        //sickness.setSelected(isSelected);
                                        //DbManager.getInstance(getContext()).selectRecord(DbManager.SICKNESSES, sickness, sickness.getId());
                                    }
                                });
                                sicknessesDialog.show(getFragmentManager(), "");
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Toast.makeText(getContext(), "Kết nối yếu...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onNegativeButtonClickListener() {

                    }
                });
                dialog.show(getFragmentManager(), "");
            }
        });
    }

    void callQuestion(final String category, String value){
        RestAIServices.getInstance().getServices().getQuestion(category, value, new Callback<String>() {
            @Override
            public void success(String responseBody, Response response) {
                String value = responseBody;
                if (Question.isQuestion(value)){
                    Question question = new Question(value);
                    code += question.getQuestionContaint() + "|";
                    SicknessQuestionDialog sicknessQuestionDialog = new SicknessQuestionDialog(question, new SicknessQuestionDialog.OnAnswerListener() {
                        @Override
                        public void onAnswerListener(String value) {
                            code += value.split("\\*")[1] + "*";
                            callQuestion(category, value);
                        }
                    });
                    sicknessQuestionDialog.show(getActivity().getSupportFragmentManager(),"");
                }
                else{
                    String name = value.split("\\*")[1].split("\\.")[0];
                    final int id = Integer.parseInt(value.split("\\*")[1].split("\\.")[1]);

                    code += name + "_" + id;
                    ContentValues values = new ContentValues();
                    values.put("RESULT",name);
                    values.put("CODE",code);
                    values.put("DATE", new SimpleDateFormat("HH:mm dd/MM/yyyy").format(Calendar.getInstance().getTime()));
                    DbManager.getInstance(getContext()).insertRecord(DbManager.DIAGNOSIS, values);
                    QuestionDialog questionDialog = new QuestionDialog("Gợi ý:", "Có thể bé đã bị " + name, "Tìm hiểu", new QuestionDialog.OnTwoChoicesSelection() {
                        @Override
                        public void onPositiveButtonClick() {
                            RestServices.getInstance().getServices().getSickness(id, new Callback<Sickness>() {
                                @Override
                                public void success(Sickness sickness, Response response) {
                                    SicknessInfoDialog dialog = new SicknessInfoDialog(sickness, new SicknessInfoDialog.OnClickListener() {
                                        @Override
                                        public void onSelectListener(Sickness sickness, boolean isSelected) {
                                            sickness.setSelected(isSelected);
                                            DbManager.getInstance(getContext()).selectRecord(DbManager.SICKNESSES, sickness, sickness.getId());
                                        }
                                    });
                                    dialog.show(getFragmentManager(), "");
                                }

                                @Override
                                public void failure(RetrofitError error) {

                                }
                            });
                        }

                        @Override
                        public void onNegativeButtonClick() {

                        }
                    });
                    questionDialog.show(getActivity().getSupportFragmentManager(),"");
                }
            }

            @Override
            public void failure(RetrofitError error) {
                QuestionDialog questionDialog = new QuestionDialog("Thông tin tín hiệu", "Không thế kết nối đến máy chủ. Vui lòng thử lại.", new QuestionDialog.OnOneChoiceSelection() {
                    @Override
                    public void onButtonClick() {

                    }
                });
                questionDialog.show(getActivity().getSupportFragmentManager(),"");
            }
        });
    }

    public void back(){
        srlSickness.setVisibility(View.GONE);
        srlCategories.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGetCategoriesSuccess(List<SicknessCategory> categories) {
        this.categories.clear();
        this.categories.addAll(categories);
        adapter.notifyDataSetChanged();
        hideLoading();
        if (srlCategories.isRefreshing()) {
            srlCategories.setRefreshing(false);
        }
    }

    @Override
    public void onGetCategoriesFailure(String error) {
        if (srlCategories.isRefreshing()) {
            srlCategories.setRefreshing(false);
        }
        hideLoading();
        QuestionDialog questionDialog = new QuestionDialog("Thông tin tín hiệu", "Không thế kết nối đến máy chủ. Vui lòng thử lại.", "Thử lại", new QuestionDialog.OnTwoChoicesSelection() {
            @Override
            public void onPositiveButtonClick() {
                presenter.getCategories();
            }

            @Override
            public void onNegativeButtonClick() {

            }
        });
        questionDialog.show(getActivity().getSupportFragmentManager(),"");
    }

    @Override
    public void showLoading() {
        progressDialog.show();
    }

    @Override
    public void hideLoading() {
        progressDialog.hide();
    }

    /**/
    public interface OnClickListener{
        void onResearchClickListener(Sickness sickness);
        void onNoteClickListener(Sickness sickness, boolean isSelected);
    }

    public class SicknessesAdapter extends RecyclerView.Adapter<SicknessesAdapter.SicknessViewHolder>{
        private List<Sickness> sicknesses = new ArrayList<Sickness>();
        OnClickListener onClickListener;

        public SicknessesAdapter(List<Sickness> sicknesses, OnClickListener onClickListener){
            this.sicknesses = sicknesses;
            this.onClickListener = onClickListener;
        }

        @Override
        public SicknessViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View itemView = inflater.inflate(R.layout.item_fragment_sickness, parent, false);
            return new SicknessViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final SicknessViewHolder holder, final int position) {
            final Sickness sickness = sicknesses.get(position);

            String urlImage = sickness.getImageURL().contains("~") ? RestServices.URL + sickness.getImageURL().replace("~/", "") : sickness.getImageURL();
            Picasso.with(FragmentSickness.this.getContext()).load(urlImage).fit().into(holder.ivSickness);
            holder.tvName.setText(sickness.getName());
            holder.tvPrognostic.setText(sickness.getPrognostic());
            holder.tvSummary.setText(sickness.getSummary());
            holder.tvResearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.onResearchClickListener(sickness);
                }
            });

            List<Sickness> sicknesses = DbManager.getInstance(getContext()).getRecords(DbManager.SICKNESSES, Sickness.class);
            boolean isSelected = false;
            if (sicknesses != null){
                for (Sickness s: sicknesses
                     ) {
                    if (s.getId() == sickness.getId()){
                        isSelected = s.isSelected();
                    }
                }
            }
            holder.cbNote.setChecked(isSelected);

            holder.cbNote.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    onClickListener.onNoteClickListener(sickness, b);
                }
            });
        }

        @Override
        public int getItemCount() {
            try {
                return sicknesses.size();
            }
            catch (Exception e){

            }
            return 0;
        }

        public class SicknessViewHolder extends RecyclerView.ViewHolder{
            @BindView(R.id.ll_item)
            LinearLayout llItem;
            @BindView(R.id.tv_research)
            TextView tvResearch;
            @BindView(R.id.iv_sickness)
            ImageView ivSickness;
            @BindView(R.id.tv_name)
            TextView tvName;
            @BindView(R.id.tv_prognostic)
            TextView tvPrognostic;
            @BindView(R.id.tv_summary)
            TextView tvSummary;
            @BindView(R.id.cb_note)
            CheckBox cbNote;

            public SicknessViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
