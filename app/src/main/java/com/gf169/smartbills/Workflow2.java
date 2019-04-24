package com.gf169.smartbills;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

import static com.gf169.smartbills.Common.OM;
import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Utils.message;

public class Workflow2 {
    static final String TAG = "gfWorkflow2";

    public static class Workflow {
        @JsonProperty("id")
        public String id;
        @JsonProperty("name")
        public String name;
        @JsonProperty("code")
        public String code;
        @JsonProperty("entity_name")
        public String entityName;
        @JsonProperty("steps")
        public ArrayList<Step> steps = null;
        @JsonProperty("order")
        public Integer order;
    }

    public static class Action {
        @JsonProperty("id")
        public String id;
        @JsonProperty("caption")
        public String caption;
        @JsonProperty("icon")
        public String icon;
        @JsonProperty("always_enabled")
        public Boolean alwaysEnabled;
        @JsonProperty("style")
        public String style;
        @JsonProperty("order")
        public Integer order;
    }

    public static class BrowserColumn {
        @JsonProperty("id")
        public String id;
        @JsonProperty("caption")
        public String caption;
        @JsonProperty("order")
        public Integer order;
    }

    public static class EditorField {
        @JsonProperty("id")
        public String id;
        @JsonProperty("caption")
        public String caption;
    }

    public static class Step {
        @JsonProperty("id")
        public String id;
        @JsonProperty("name")
        public String name;
        @JsonProperty("entity_name")
        public String entityName;
        @JsonProperty("permission")
        public String permission;
        @JsonProperty("actions")
        public ArrayList<Action> actions = null;
        @JsonProperty("browser_columns")
        public ArrayList<BrowserColumn> browserColumns = null;
        @JsonProperty("editor_fields")
        public ArrayList<EditorField> editorFields = null;
        @JsonProperty("order")
        public Integer order;
    }

    private static ArrayList<Workflow> workflows;

    private static Workflow getWorkflow(String workflowId) {
        if (workflows == null) {
            if (cr.getSomething("all", 3)) {
                try {
                    workflows = OM.readValue(cr.responseBodyStr,
                            OM.getTypeFactory().constructCollectionType(ArrayList.class, Workflow.class));
                } catch (Exception e) {
                    message(e.toString());
                    e.printStackTrace();
                    return null;
                }
            } else {
                message("Ошибка при получении описания workflow");
                return null;
            }
        }
        for (int i = 0; i < workflows.size(); i++) {
            if (workflows.get(i).id.equals(workflowId)) {
                return workflows.get(i);
            }
        }
        return null;
    }

    static ArrayList<String> getWorkflowActions(String workflowId, String stepName) {
        Log.d(TAG, "getWorkflowActions stepName=" + stepName);

        Workflow workflow = getWorkflow(workflowId);
        ArrayList<String> r = new ArrayList<>();

        if (workflow != null && workflow.steps != null) {
            for (Step step : workflow.steps) {
                if (step.name.equals(stepName) && step.actions != null) {
                    for (Action action : step.actions) {
                        r.add(action.caption + "=" + step.id + "=" + action.id);
                        Log.d(TAG, "getWorkflowActions: " + action.caption);
                    }
                    return r;
                }
            }
        }
        return r;
    }

    static ArrayList<String> getEditableFields(String workflowId, String stepName) {
        Log.d(TAG, "getEditableFields stepName=" + stepName);

        Workflow workflow = getWorkflow(workflowId);
        ArrayList<String> r = new ArrayList<>();

        if (workflow != null && workflow.steps != null) {
            for (Step step : workflow.steps) {
                if (step.name.equals(stepName) && step.editorFields != null) {
                    for (EditorField editorField : step.editorFields) {
                        r.add(editorField.caption);
                        Log.d(TAG, "getgetEditableFields: " + editorField.caption);
                    }
                    return r;
                }
            }
        }
        return r;
    }
}
