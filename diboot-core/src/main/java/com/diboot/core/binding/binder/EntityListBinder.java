/*
 * Copyright (c) 2015-2020, www.dibo.ltd (service@dibo.ltd).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.diboot.core.binding.binder;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diboot.core.binding.annotation.BindEntityList;
import com.diboot.core.binding.helper.ResultAssembler;
import com.diboot.core.config.Cons;
import com.diboot.core.exception.InvalidUsageException;
import com.diboot.core.util.BeanUtils;
import com.diboot.core.util.S;
import com.diboot.core.util.V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Entity集合绑定实现
 * @author mazc@dibo.ltd
 * @version v2.0
 * @date 2019/1/19
 */
public class EntityListBinder<T> extends EntityBinder<T> {
    private static final Logger log = LoggerFactory.getLogger(EntityListBinder.class);

    /***
     * 构造方法
     * @param serviceInstance
     * @param voList
     * @param annotation
     */
    public EntityListBinder(IService<T> serviceInstance, List voList, BindEntityList annotation){
        super(serviceInstance, voList);
        if(V.notEmpty(annotation.splitBy())){
            this.splitBy = annotation.splitBy();
        }
        if(V.notEmpty(annotation.orderBy())){
            this.orderBy = annotation.orderBy();
        }
    }

    @Override
    public void bind() {
        if(V.isEmpty(annoObjectList)){
            return;
        }
        if(V.isEmpty(refObjJoinCols)){
            throw new InvalidUsageException("调用错误：无法从condition中解析出字段关联.");
        }
        Map<String, List> valueEntityListMap = new HashMap<>();
        if(middleTable == null){
            super.simplifySelectColumns();
            super.buildQueryWrapperJoinOn();
            //处理orderBy，附加排序
            this.appendOrderBy();
            // 查询entity列表
            List<T> list = getEntityList(queryWrapper);
            if(V.notEmpty(list)){
                valueEntityListMap = this.buildMatchKey2EntityListMap(list);
            }
            ResultAssembler.bindPropValue(annoObjectField, annoObjectList, getAnnoObjJoinFlds(), valueEntityListMap, this.splitBy);
        }
        else{
            if(refObjJoinCols.size() > 1){
                throw new InvalidUsageException(NOT_SUPPORT_MSG);
            }
            // 提取注解条件中指定的对应的列表
            Map<String, List> trunkObjCol2ValuesMap = super.buildTrunkObjCol2ValuesMap();
            Map<String, List> middleTableResultMap = middleTable.executeOneToManyQuery(trunkObjCol2ValuesMap);
            if(V.isEmpty(middleTableResultMap)){
                return;
            }
            super.simplifySelectColumns();
            // 收集查询结果values集合
            List entityIdList = extractIdValueFromMap(middleTableResultMap);
            if(V.notEmpty(this.splitBy)){
                entityIdList = ResultAssembler.unpackValueList(entityIdList, this.splitBy);
            }
            // 构建查询条件
            queryWrapper.in(refObjJoinCols.get(0), entityIdList);
            //处理orderBy，附加排序
            this.appendOrderBy();
            // 查询entity列表: List<Role>
            List list = getEntityList(queryWrapper);
            if(V.isEmpty(list)){
                return;
            }
            String refObjJoinOnField = toRefObjField(refObjJoinCols.get(0));
            // 转换entity列表为Map<ID, Entity>
            Map<String, T> entityMap = BeanUtils.convertToStringKeyObjectMap(list, refObjJoinOnField);
            for(Map.Entry<String, List> entry : middleTableResultMap.entrySet()){
                // List<roleId>
                List annoObjFKList = entry.getValue();
                if(V.isEmpty(annoObjFKList)){
                    continue;
                }
                List valueList = new ArrayList();
                for(Object obj : annoObjFKList){
                    if(obj == null){
                        continue;
                    }
                    String valStr = String.valueOf(obj);
                    T ent = entityMap.get(valStr);
                    if(ent != null){
                        valueList.add(cloneOrConvertBean(ent));
                    }
                    else if(V.notEmpty(splitBy) && valStr.contains(splitBy)){
                        for(String key : valStr.split(splitBy)){
                            ent = entityMap.get(key);
                            if(ent != null){
                                valueList.add(cloneOrConvertBean(ent));
                            }
                        }
                    }
                }
                valueEntityListMap.put(entry.getKey(), valueList);
            }
            // 绑定结果
            ResultAssembler.bindEntityPropValue(annoObjectField, annoObjectList, middleTable.getTrunkObjColMapping(), valueEntityListMap, getAnnoObjColumnToFieldMap());
        }
    }

    /**
     * 构建匹配key-entity目标的map
     * @param list
     * @return
     */
    private Map<String, List> buildMatchKey2EntityListMap(List<T> list){
        Map<String, List> key2TargetListMap = new HashMap<>(list.size());
        StringBuilder sb = new StringBuilder();
        for(T entity : list){
            sb.setLength(0);
            for(int i=0; i<refObjJoinCols.size(); i++){
                String refObjJoinOnCol = refObjJoinCols.get(i);
                String pkValue = BeanUtils.getStringProperty(entity, toRefObjField(refObjJoinOnCol));
                if(i > 0){
                    sb.append(Cons.SEPARATOR_COMMA);
                }
                sb.append(pkValue);
            }
            // 查找匹配Key
            String matchKey = sb.toString();
            // 获取list
            List entityList = key2TargetListMap.get(matchKey);
            if(entityList == null){
                entityList = new ArrayList<>();
                key2TargetListMap.put(matchKey, entityList);
            }
            Object target = entity;
            if(target instanceof Map == false){
                target = cloneOrConvertBean(entity);
            }
            entityList.add(target);
        }
        sb.setLength(0);
        return key2TargetListMap;
    }

}
