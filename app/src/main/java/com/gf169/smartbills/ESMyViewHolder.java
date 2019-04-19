package com.gf169.smartbills;

import android.view.View;
import android.widget.TextView;

import static com.gf169.smartbills.Common.SDF_OUR;
import static com.gf169.smartbills.Common.mainActivity;
import static com.gf169.smartbills.ESEntityLists.MY_QUERIES;
import static com.gf169.smartbills.ESEntityLists.MY_SUBORDINATES_QUERIES;

class ESMyViewHolder extends MainActivity.MyViewHolder {
    private TextView vNumber;
    private TextView vDate;
    private TextView vCompany;
    private TextView vSum;
    private TextView vProject;
    private TextView vClause;
    private TextView vComment;
    private TextView vStep;

    ESMyViewHolder(View v) {
        mainActivity.super(v);           // curActivity !!!

        vNumber = v.findViewById(R.id.textViewNumber);
        vDate = v.findViewById(R.id.textViewDate);
        vCompany = v.findViewById(R.id.textViewCompany);
        vSum = v.findViewById(R.id.textViewSum);
        vProject = v.findViewById(R.id.textViewProject);
        vClause = v.findViewById(R.id.textViewClause);
        vComment = v.findViewById(R.id.textViewComment);

        vStep = v.findViewById(R.id.textViewStep);
        if (!mainActivity.listKindNames.get(mainActivity.entityListNumber).equals(MY_QUERIES) &&
                !mainActivity.listKindNames.get(mainActivity.entityListNumber).equals(MY_SUBORDINATES_QUERIES)) {
            vStep.setVisibility(View.GONE);
        }
    }

    @Override
    protected void fillFields(Object entity, boolean isMarked, boolean isSelected) {
        Entities.Query query = (Entities.Query) entity;  // Entity Specific
        super.fillFields(query, isMarked, isSelected);

        vNumber.setText(query.number);
        vDate.setText(SDF_OUR.format(query.createTs));
        if (query.company != null) vCompany.setText(query.company.name);
        vSum.setText(query.amount.toString());
        if (query.project != null) vProject.setText(query.project.name);
        if (query.clause != null) vClause.setText(query.clause.name);
        vComment.setText(query.comment);
        vStep.setText(query.stepName);

        vIsMarked.setChecked(isMarked);

        paintItem(v, isSelected, query);
    }
/*
    @Override
    protected void paintItem(View v, boolean isSelected, Object entity) {
        Entities.Query query = (Entities.Query) entity; // Entity Specific

        int color = curActivity.getResources().getColor(isSelected ?
                query.urgent ? R.color.colorItemUrgentSelected : R.color.colorItemSelected :
                query.urgent ? R.color.colorItemUrgent : R.color.colorItem
        );
        v.setBackgroundColor(color);
    }
*/
}
