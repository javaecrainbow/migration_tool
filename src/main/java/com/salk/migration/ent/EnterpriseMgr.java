package com.salk.migration.ent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * <p/>
 * 企业管理
 * <p/>
 *
 * @author salkli
 * @date 2022/4/12
 */

@Component("EnterpriseMgr")
public class EnterpriseMgr {

    @Resource
    JdbcTemplate jdbcTemplate;

    private static Map<String, List<String>> entMap = new HashMap();

    public void addEntId(String... entIds) {

        // entIdList.addAll(Arrays.asList(entIds));
    }

    public void getEntId(String... entIds) {
        // entIdList.addAll(Arrays.asList(entIds));
    }

    public static List<String> getEntId(String env) {
        return entMap.get(env);
    }

    public EnterpriseMgr() {
        // init();
    }

    public EnterpriseMgr(String jdbcUrl, String userName, String password) {
        init();
    }

    public void init() {


        List<String> entIds = new ArrayList<>();
        entIds.addAll(Arrays.asList("apiceshi0c", "APICeShiQiYe"));
        entMap.put("stable", entIds);
        // entMap.put("stable", stableList);
        List<String> entIds2 = new ArrayList<>();
        entIds2.addAll(Arrays.asList("apiceshi0c", "apiceshi0c"));
        entMap.put("gray", entIds2);
        // entMap.put("gray", grayList);
        List<String> entIds3 = new ArrayList<>();
        entIds3.addAll(Arrays.asList("apiceshi0c", "apiceshi0c"));
        entMap.put("prefix", entIds3);
    }

}
