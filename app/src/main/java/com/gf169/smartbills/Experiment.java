package com.gf169.smartbills;

class Experiment {
    boolean exec() {
//        cr.getSomething("metadata/entities/bills$Query/views"); if (true) return true;
//        cr.getSomething("metadata/entities/bills$Query"); if (true) return true;
//        Set<Entities.Query> set=searchEntities("bills$Query", Entities.Query.class,"query-browse","number","contains","74",null); if (true) {
//            return true;
//        }

//        cr.getSomething("metadata/entities/bills$ExtUser/views"); if (true) return true;
//        cr.getEntitiesList("bills$ExtUser",null,"user-constraint-data",0,0,null,true,true,true); if (true) return true;

//        cr.getSomething("metadata/entities/sec$UserRole/views"); if (true) return true;
//        cr.getEntitiesList("sec$UserRole",null,null,0,0,null,true,true,true); if (true) return true;

//        cr.getSomething("metadata/entities/bills$ExtRole/views"); if (true) return true;
//        cr.getEntitiesList("bills$ExtRole",null,"extRole-edit",0,0,null,true,true,true); if (true) return true;

//        cr.getSomething("metadata/entities/wfstp$Stage/views"); if (true) return true;
//        cr.getEntitiesList("wfstp$Stage",null,"stage-notification",0,0,null,true,true,true); if (true) return true;

//        cr.getSomething("metadata/entities/bills$Employee/views"); if (true) return true;
//        cr.getEntitiesList("bills$Employee",null,"employee-browse",0,0,null,true,true,true); if (true) return true;
//        searchEntities("bills$Employee", Entities.Employee.class,"employee-browse","name","=","Gf16954@gmail.com",null); if (true) return true;
//        searchEntities("bills$Employee", Entities.Employee.class,"employee-browse",
//                null, null, null, "_entityname"); if (true) return true;
// Куба отказывается сортировать по _entityName и _instanceName - 500 без объяснений. По остальным полям ОК
//        cr.getSomething("metadata/entities/bills$Workflow/views"); if (true) return true;
//        cr.getEntitiesList("wfstp$Workflow",null,"query-workflow-browse",0,0,null,true,true,true); if (true) return true;

//        cr.getSomething("metadata/entities/bills$Project/views"); if (true) return true;
//        cr.getEntitiesList("bills$Project",null,null,0,0,null,true,true,true); if (true) return true;

//        cr.getSomething("queries/bills$Query"); if (true) return true;  // Один - поиск по номеру

//        cr.getSomething("services"); if (true) return true; // Пусто

//        cr.getSomething("metadata/enums"); if (true) return true;
// {"name":"com.groupstp.workflowstp.entity.WorkflowEntityStatus","values":[{"name":"IN_PROGRESS","id":1,"caption":"В процессе"},{"name":"DONE","id":2,"caption":"Завершена"},{"name":"FAILED","id":3,"caption":"Ошибка"}]}

/*
        if (cr.execJPQLPost("bills$Query"
                , "byExtKey", "{\"extKey\":\"ИА-00000074\"}","query-browse", 50
                , 0, false, true, true)) {
            try {
                ArrayList<Entities.Query> a=om.readValue(cr.responseBodyStr,
                        new TypeReference<ArrayList<Entities.Query>>() {});
            } catch (Exception e) {
                e.printStackTrace();
                message(e.getMessage());
            }
        } else {
            Utils.message(TAG,"Не удалось получить список экземпляров");
        }
        if (true) return true;
*/
/*
        try {
            Set<FilterDialogFragment.QueryField> entityFields = MainActivity.activity.om.readValue(
                    "[{\"name\":\"inBudget\"}]",
                    new TypeReference<Set<FilterDialogFragment.QueryField>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        if (true) return true;
*/
/*
        try {
            Class c=Class.forName("com.gf169.smartbills.Entities$Query");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (true) return true;
*/
/*
        class A {
            int i;
            Date d=new Date();
        }
        A a=new A();
        Log.d("qqqq",""+a.i+" "+a.d);  // 0
*/
/* Плохо! select from select

//        cr.execJPQLPost("bills$Query","byInitiators",
//                     "{\"initiatorsIds\":[77ac7758-a106-8fe4-d956-0925f087517c]}",
//                null, 3, 3, true, false, true);
        cr.getEntitiesList("bills$Query",
               "{\"conditions\":[{" +
                    "\"property\":\"initiator\"," +
                    "\"operator\":\"=\"," +
                    "\"value\":\"77ac7758-a106-8fe4-d956-0925f087517c\"" +
                    "}]}",
                    null, 3, 6, "number", false, true,true);
        if (null == null) return true;
*/
/*
        for (int i=0; i<100; i++) logD2("qqqqqqq"); // 0.215-0.055 сек  Без formLogTag() 0.01 сек
        logD2("qqqqqqq"); if (true) return true;
*/
        return false;
    }
}
