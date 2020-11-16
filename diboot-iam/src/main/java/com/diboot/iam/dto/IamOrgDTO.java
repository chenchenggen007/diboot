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
package com.diboot.iam.dto;

import com.diboot.core.binding.query.BindQuery;
import com.diboot.core.binding.query.Comparison;
import com.diboot.iam.entity.IamOrg;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * IamOrg的DTO定义
 * @author mazc@dibo.ltd
 * @version 2.0
 * @date 2020-11-03
 */
@Getter
@Setter
@Accessors(chain = true)
public class IamOrgDTO extends IamOrg {

    @BindQuery(comparison = Comparison.LIKE, field = "name")
    private String name;
}
