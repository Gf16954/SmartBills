package com.gf169.smartbills;

import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curUser;

class ESMisc {
    public static String mainEntityName = "bills$Query";

    public static void iniEntity(Object entity) {
        Entities.Query query = (Entities.Query) entity;

        query.initiator = Common.curEmployee;
        if (curUser.companies != null) query.company = curUser.companies.get(0);
        //        eIn.amount=555.55;
        //        eIn.number="12345";
        //        eIn.urgent=true;
    }

    public static void clearEntity(Object entity) {
        Entities.Query query = (Entities.Query) entity;

        query.id = null;
        query.number = null;
    }

    static final String[] mandatoryFields = {"Сумма", "Компания", "Инициатор"};  // Поля, обязательные к заполнению
    static final String[] editableFields = {"Номер", "Тип платежа", "Проект", "Статья ДДС", "НДС",  // ОСТАЛЬНЫЕ редактируемые поля
            "Наименование поставщика", "ИНН", "Срок платежа", "Назначение платежа",   // ,"Наличие в бюджете"
            "Срочность", "Комментарий"};  // TODO: 23.03.2019 Добавить Вложения

    public static String processEntity(Object entity, String sOk, String sNotOk) {
        Entities.Query query = (Entities.Query) entity;

        if (((Common.GetWorkflow) query).getStepName() == null) {  // В работу
            if (cr.postJSON("query/process?id=" +
                    query.getId(), "{}", true)) {
                return sOk;
            } else {
                return sNotOk + cr.error;
            }
        }
        return "ошибка: не умеем обрабатывать заявку в этом статусе";
    }
}
