package com.gf169.smartbills;

// Generated here: http://www.jsonschema2pojo.org/

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.gf169.smartbills.Common.Get;
import static com.gf169.smartbills.Common.GetWorkflow;
import static com.gf169.smartbills.Common.OM;
import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curActivity;
import static com.gf169.smartbills.Common.searchEntities;
import static com.gf169.smartbills.ESEntityLists.PROBLEM_QUERIES;
import static com.gf169.smartbills.GetPath.getPath;
import static com.gf169.smartbills.Utils.copyFile;
import static com.gf169.smartbills.Utils.message;

//import static com.gf169.smartbills.Utils.getPath;

class Entities {
    static final String TAG = "gfEntities";

    public static class Query implements Get, GetWorkflow {

        @JsonProperty("_entityName")
        public String _entityName;   // bills$Query
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("inBudget")
        public Boolean inBudget;
        @JsonProperty("purpose")
        public Object purpose;
        @JsonProperty("initiator")
        public Employee initiator;
        @JsonProperty("dueDate")
        public Date dueDate;
        @JsonProperty("project")
        public Project project;
        @JsonProperty("paymentType")
        public String paymentType;
        @JsonProperty("number")
        public String number;
        @JsonProperty("stepName")
        public String stepName;
        //        @JsonProperty("supplier")  // Не используется
//        public Object supplier;
        @JsonProperty("createTs")
        public Date createTs;      // В оригинале DateTime
        @JsonProperty("company")
        public Company company;
        @JsonProperty("urgent")
        public Boolean urgent;
        @JsonProperty("clause")
        public Clause clause;
        @JsonProperty("amount")
        public Double amount;
        @JsonProperty("images")
        public List<ExternalFileDescriptor> images = null;
        @JsonProperty("workflow")
        public Workflow workflow;
        @JsonProperty("taxNumber")
        public Object taxNumber;
        @JsonProperty("supplierAbout")
        public String supplierAbout;
        @JsonProperty("vatIncluded")
        public Boolean vatIncluded;
        @JsonProperty("comment")
        public String comment;
        @JsonProperty("paymentDate")
        public Date paymentDate;   // !!! Без времени
        @JsonProperty("status")
        public String status;

        Query() {
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName != null && !_instanceName.trim().isEmpty() ? _instanceName.trim() : number;
        }

        @Override
        public String toString() {
            return number == null ? "Новая заявка" : "Заявка #" + number;
        }

        @Override
//        public String getWorkflow() { Если есть такой getter, то при serialization его использует, а не берет непосредственно значение поле
        public String getWorkflowId() {
            return workflow == null ? null : workflow.id;
        }

        @Override
        public String getStepName() {
            return stepName;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public boolean hasActor(ExtUser extUser) {
            return (stepName == null || stepName.equals(PROBLEM_QUERIES)) &&
                    initiator.user.equals(extUser);
        }
    }

    public static class Clause implements Get {

        @JsonProperty("_entityName")
        public String _entityName;
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("number")
        public String number;
        @JsonProperty("name")
        public String name;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            // return _instanceName!=null && !_instanceName.trim().isEmpty() ? _instanceName.trim() : name;
            return number + ": " + name;
        }
    }

    public static class Company implements Get {

        @JsonProperty("_entityName")
        public String _entityName;
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("budgetSpreadSheetUrl")
        public String budgetSpreadSheetUrl;
        @JsonProperty("code")
        public String code;
        @JsonProperty("paymentIdentity")
        public String paymentIdentity;
        @JsonProperty("budgetAgreed")
        public Boolean budgetAgreed;
        @JsonProperty("fullName")
        public String fullName;
        @JsonProperty("taxNumber")
        public String taxNumber;
        @JsonProperty("taxCode")
        public String taxCode;
        @JsonProperty("name")
        public String name;
        @JsonProperty("comment")
        public Object comment;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName != null && !_instanceName.trim().isEmpty() ? _instanceName.trim() : name;
        }
    }

    public static class ExternalFileDescriptor implements Get {

        @JsonProperty("_entityName")
        public String _entityName;   // bills$ExternalFileDescriptor
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("extension")
        public String extension;
        @JsonProperty("externalCode")
        public String externalCode;
        @JsonProperty("name")
        public String name;
        @JsonProperty("createDate")
        public Date createDate;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return name;    // В _instanceName еще прицеплено время создания в конце
        }

        @Override
        public String toString() {
            return getInstanceName();
        }

        ExternalFileDescriptor() {
        }

        // Если есть какой-нибудь конструктор, то должен быть и пустой, иначе Object Mapper отваливается

        ExternalFileDescriptor(Uri uri) {
            externalCode = getPath(curActivity, uri); // full path! Только при создании, с сервера приедет null
            if (externalCode != null) { // && externalCode.contains("/")) {
                _instanceName = name = externalCode.substring(externalCode.lastIndexOf("/") + 1);  // file name

            } else { // Вероятно, файл из облака (Google drive, например) - загружаем
//                 if ("com.google.android.apps.docs.storage".equals(uri.getAuthority())) {
//                 }
                String mimeType = curActivity.getContentResolver().getType(uri);
                String ext = "." + mimeType.substring(mimeType.lastIndexOf("/") + 1);
                Cursor cursor = null;
                try {
                    cursor = curActivity.getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        String fileName = cursor.getString(nameIndex);

                        String fullPath = curActivity.getFilesDir().getAbsolutePath() + "/temp" + ext;
                        if (copyFile(uri, fullPath)) {
                            externalCode = fullPath;
                            _instanceName = name = fileName;
                        }
                    }
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            }
        }
    }

    public static class Employee implements Get {

        @JsonProperty("_entityName")
        public String _entityName;  // bills$Employee
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("name")
        public String name;
        @JsonProperty("fullName")
        public String fullName;
        @JsonProperty("email")
        public String email;
        @JsonProperty("user")
        public ExtUser user;
        @JsonProperty("manager")
        public Employee manager;
        @JsonProperty("subordinates")
        public List<Employee> subordinates;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName != null && !_instanceName.trim().isEmpty() ? _instanceName.trim() :
                    name != null && !name.trim().isEmpty() ? name.trim() :
                            email;
        }

        public boolean equals(Object o) {
            return o != null && ((Employee) o).id.equals(id);
        }


        void fillSubordinates() {
            subordinates = searchEntities(
                    "bills$Employee", Employee.class, "employee-hierarchy",
                    "id", "=", id, null).toArray(new Employee[0])[0].subordinates;
        }
    }

    public static class Project implements Get {

        @JsonProperty("_entityName")
        public String _entityName;
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("name")
        public String name;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName != null && !_instanceName.trim().isEmpty() ? _instanceName.trim() : name;
        }
    }

    public static class Workflow implements Get {

        @JsonProperty("_entityName")
        public String _entityName;
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("code")
        public String code;
        @JsonProperty("entityName")
        public String entityName;
        @JsonProperty("name")
        public String name;
        @JsonProperty("steps")
        public List<Step> steps = null;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName != null && !_instanceName.trim().isEmpty() ? _instanceName.trim() : name;
        }
    }

    public static class Step implements Get {

        @JsonProperty("_entityName")
        public String _entityName;
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("stage")
        public Stage stage;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName;
        }
    }

    public static class UserInfo {

        @JsonProperty("id")
        public String id;
        @JsonProperty("login")
        public String login;
        @JsonProperty("name")
        public String name;
        @JsonProperty("firstName")
        public String firstName;
        @JsonProperty("middleName")
        public String middleName;
        @JsonProperty("lastName")
        public String lastName;
        @JsonProperty("position")
        public String position;
        @JsonProperty("email")
        public String email;
        @JsonProperty("timeZone")
        public String timeZone;
        @JsonProperty("language")
        public String language;
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("locale")
        public String locale;

        static UserInfo build() {
            Log.d(TAG, "UserInfo.build");

            if (cr.getSomething("userInfo")) {
                try {
                    return OM.readValue(
                            cr.responseBodyStr, new TypeReference<UserInfo>() {
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                    message(e.getMessage());
                }
            } else {
                Log.d(TAG, "Не определился текущий пользователь");
            }
            return null;
        }
    }

    public static class Stage implements Get {

        @JsonProperty("_entityName")
        public String _entityName;  // wfstp$Stage
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("type")
        public String type;
        @JsonProperty("version")
        public Integer version;
        @JsonProperty("actors")
        public List<ExtUser> actors = null;
        @JsonProperty("actorsRoles")
        public List<ExtRole> actorsRoles = null;
        @JsonProperty("viewers")
        public List<ExtUser> viewers = null;
        @JsonProperty("viewersRoles")
        public List<ExtRole> viewersRoles = null;
        @JsonProperty("directionVariables")
        public String directionVariables;
        @JsonProperty("name")
        public String name;
        @JsonProperty("entityCaption")
        public String entityCaption;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName != null && !_instanceName.trim().isEmpty() ? _instanceName.trim() : name;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && ((Stage) o).id.equals(id);
        }

        static Stage build(CharSequence name) {
            Log.d(TAG, "Stage.build " + name);

            ArrayList<Stage> s = searchEntities(
                    "wfstp$Stage", Stage.class, "stage-edit",  // Очень подробный
                    "name", "=", name.toString(), null);
            if (CollectionUtils.isEmpty(s)) {
                message("Stage " + name + " не найдена");
                return null;
            } else {
                return s.toArray(new Stage[0])[0];
            }
        }

        boolean isFinal() {
            return false; // ToDo
        }
    }

    public static class ExtUser implements Get {

        @JsonProperty("_entityName")
        public String _entityName;     // bills$ExtUser
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("lastName")
        public String lastName;
        @JsonProperty("projects")
        public List<Project> projects = null;
        @JsonProperty("loginLowerCase")
        public String loginLowerCase;
        @JsonProperty("ipMask")
        public Object ipMask;
        @JsonProperty("language")
        public Object language;
        @JsonProperty("login")
        public String login;
        @JsonProperty("password")
        public Object password;
        @JsonProperty("changePasswordAtNextLogon")
        public Boolean changePasswordAtNextLogon;
        @JsonProperty("companies")
        public List<Company> companies = null;
        @JsonProperty("timeZoneAuto")
        public Object timeZoneAuto;
        @JsonProperty("email")
        public String email;
        @JsonProperty("disablePasswordLogin")
        public Object disablePasswordLogin;
        @JsonProperty("timeZone")
        public Object timeZone;
        @JsonProperty("active")
        public Boolean active;
        @JsonProperty("firstName")
        public String firstName;
        @JsonProperty("userRoles")
        public List<UserRole> userRoles = null;
        @JsonProperty("name")
        public String name;
        @JsonProperty("middleName")
        public Object middleName;
        @JsonProperty("position")
        public Object position;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName != null && !_instanceName.trim().isEmpty() ? _instanceName.trim() : name;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && ((ExtUser) o).id.equals(id);
        }

        static ExtUser build(String id) {
            Log.d(TAG, "ExtUser.build");

            if (id == null) {  // Текущий юзер
                UserInfo userInfo = UserInfo.build();
                if (userInfo == null) {
                    message("Не удалось получить параметры текущего пользователя");
                    return null;
                }
                id = userInfo.id;
            }
            ArrayList<ExtUser> s = searchEntities(
                    "bills$ExtUser", ExtUser.class, "user-constraint-data",
                    "id", "=", id, null);
            if (CollectionUtils.isEmpty(s)) {
                message("Пользователь " + id + " не найден");
                return null;
            } else {
                return s.toArray(new ExtUser[0])[0];
            }
        }

        static ExtUser buildByLogin(String login) {
            Log.d(TAG, "ExtUser.build");

            ArrayList<ExtUser> s = searchEntities(
                    "bills$ExtUser", ExtUser.class, "user-constraint-data",
                    "login", "=", login, null);
            if (CollectionUtils.isEmpty(s)) {
                message("Пользователь " + login + " не найден");
                return null;
            } else {
                return s.toArray(new ExtUser[0])[0];
            }
        }

        Employee getEmployee() {
            ArrayList<Employee> s = searchEntities(
                    "bills$Employee", Employee.class,
                    null, "user", "=", id, null);
            if (CollectionUtils.isEmpty(s)) {
                message("Сотрудник для юзера " + id + " не найден");
                return null;
            }
            return s.toArray(new Employee[0])[0];
        }

        Set<ExtRole> getRoles() {
            Log.d(TAG, "ExtUser.getRoles");

            if (CollectionUtils.isEmpty(userRoles)) return Collections.emptySet();

            Set<ExtRole> result = new HashSet<>();
            for (UserRole ur : userRoles) {  // ToDo Переделать - stream
                result.add(ur.role);
            }
            Log.d(TAG, "ExtUser.getRoles " + result);
            return result;
        }

        public boolean isActor(Stage stage) {
            if (stage == null) return true;

            if (!CollectionUtils.isEmpty(userRoles)) {
                for (UserRole ur : userRoles) {
                    /* if (stage == null) {  // Право на создание и "в работу"
                        if (ur.role.name.equals(ROLE_USER)) return true;
                    } else */
                    if (!CollectionUtils.isEmpty(stage.actorsRoles) &&
                            stage.actorsRoles.contains(ur.role)) {
                        return true;
                    }
                }
            }
            return !CollectionUtils.isEmpty(stage.actors) && stage.actors.contains(this);
        }

        public Set<Company> getCompanies(Stage stage) {  // Первоисточник - UserControlWorkerBean.java
            Log.d(TAG, "ExtUser.getCompanies " + stage);

            Set<Company> result;

            if (stage == null) { // Stage не задана
                result = new HashSet<>(companies); // Компании юзера
                if (!CollectionUtils.isEmpty(userRoles)) {
                    for (UserRole ur : userRoles) {
                        if (!CollectionUtils.isEmpty(ur.role.companies)) { // Где он играет какую-то роль
                            result.addAll(ur.role.companies);
                        }
                    }
                }
                Log.d(TAG, "ExtUser.getCompanies userCompanies " + result);
                return result;
            }

            result = new HashSet<>();

            boolean userActorByRole = false;
            boolean userViewerByRole = false;

            if (!CollectionUtils.isEmpty(userRoles)) {
                for (UserRole ur : userRoles) {  // Роль пользователя ...
                    if (!CollectionUtils.isEmpty(stage.actorsRoles) &&
                            stage.actorsRoles.contains(ur.role) &&        // ... содержится в ролях этой стадии
                            (userActorByRole = true) ||  // Присвоение!
                            !CollectionUtils.isEmpty(stage.viewersRoles) &&
                                    stage.viewersRoles.contains(ur.role) &&
                                    (userViewerByRole = true)
                    ) {
                        result.addAll(ur.role.companies);  // Компании, в которых он играет эту роль
                    }
                }
            }
            if (userActorByRole || !CollectionUtils.isEmpty(stage.actors) && stage.actors.contains(this) ||
                    userViewerByRole || !CollectionUtils.isEmpty(stage.viewers) && stage.viewers.contains(this)) {
                result.addAll(companies);  // Комании юзера
            }

            Log.d(TAG, "ExtUser.getCompanies " + result);
            return result;
        }

        public Set<Project> getProjects(Stage stage) {  // Первоисточник - UserControlWorkerBean.java
            Log.d(TAG, "ExtUser.getProjects " + stage);

            Set<Project> result;

            if (stage == null) { // Stage не задан
                result = new HashSet<>(projects);
                if (!CollectionUtils.isEmpty(userRoles)) {
                    for (UserRole ur : userRoles) {
                        if (!CollectionUtils.isEmpty(ur.role.projects)) {
                            result.addAll(ur.role.projects);
                        }
                    }
                }
                Log.d(TAG, "ExtUser.getCompanies userCompanies " + result);
                return result;
            }

            result = new HashSet<>();

            boolean userActorByRole = false;
            boolean userViewerByRole = false;

            if (!CollectionUtils.isEmpty(userRoles)) {
                for (UserRole ur : userRoles) {  // Роль пользователя ...
                    if (!CollectionUtils.isEmpty(stage.actorsRoles) &&
                            stage.actorsRoles.contains(ur.role) &&        // ... содержится в ролях этой стадии
                            (userActorByRole = true) ||
                            !CollectionUtils.isEmpty(stage.viewersRoles) &&
                                    stage.viewersRoles.contains(ur.role) &&
                                    (userViewerByRole = true)
                    ) {
                        result.addAll(ur.role.projects);
                    }
                }
            }
            if (userActorByRole || !CollectionUtils.isEmpty(stage.actors) && stage.actors.contains(this) ||
                    userViewerByRole || !CollectionUtils.isEmpty(stage.viewers) && stage.viewers.contains(this)) {
                result.addAll(projects);
            }

            Log.d(TAG, "ExtUser.getProjects " + result);
            return result;
        }
    }

    public static class ExtRole implements Get {

        @JsonProperty("_entityName")
        public String _entityName;      // bills$ExtRole
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("projects")
        public List<Project> projects = null;
        @JsonProperty("defaultRole")
        public Boolean defaultRole;
        @JsonProperty("locName")
        public Object locName;
        @JsonProperty("description")
        public Object description;
        @JsonProperty("type")
        public String type;
        @JsonProperty("companies")
        public List<Company> companies = null;
        @JsonProperty("name")
        public String name;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName != null && !_instanceName.trim().isEmpty() ? _instanceName.trim() : name;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && ((ExtRole) o).id.equals(id);
        }
    }

    public static class UserRole implements Get {

        @JsonProperty("_entityName")
        public String _entityName;    // sec$UserRole
        @JsonProperty("_instanceName")
        public String _instanceName;
        @JsonProperty("id")
        public String id;
        @JsonProperty("role")
        public ExtRole role;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getInstanceName() {
            return _instanceName;
        }
    }

}
