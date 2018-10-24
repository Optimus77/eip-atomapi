package com.inspur.eipatomapi.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author: jiasirui
 * @date: 2018/10/23 14:19
 * @description:
 */
public class EntityUtil {

    public final static Logger log = LoggerFactory.getLogger(EntityUtil.class);

    public static String[] getColumnValue(Class classz) {

        Field[] fields=classz.getDeclaredFields();
        log.info("fields :{}",fields.toString());
        Field field;
        String[] value=new String[fields.length];
        for (int i = 0; i <fields.length ; i++) {
            fields[i].setAccessible(true);
        }

        for(int i = 1;i<fields.length ; i++){
            try {
                field=classz.getDeclaredField(fields[i].getName());
                Column column=field.getAnnotation(Column.class); //获取指定类型注解
                if(column!=null){
                    value[i]=column.name();
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return value;
    }
    public static String getMethodAnnotation(Class classz) {
        Method[] methods = classz.getDeclaredMethods();
        for (Method method : methods) {
            boolean methodHasAnno = method.isAnnotationPresent(Column.class);
            if (methodHasAnno) {
                Column methodAnno = method.getAnnotation(Column.class);
                String desc = methodAnno.name();
                log.info(method.getName() + " desc = " + desc);
            }

        }
        return "";
    }


    public static String getTableValue(Class classz){

        Table tableAnnotation= (Table) classz.getAnnotation(Table.class);
        return tableAnnotation.name();

    }

    public static String getCloumValue(Field field){

        Column column=field.getAnnotation(Column.class);
        return column.name();

    }

}
