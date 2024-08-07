/**
 * (c) Copyright Ascensio System SIA 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.qiwenshare.file.office.services;

import com.qiwenshare.file.office.entities.Group;
import com.qiwenshare.file.office.repositories.GroupRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//@Service
public class GroupServices
{

    //    @Autowired
    private GroupRepository groupRepository;

    // create a new group with the specified name
    public Group createGroup(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;  // check if a name is specified
        }
        Optional<Group> group = groupRepository.findGroupByName(name);  // check if group with such a name already exists
        if (group.isPresent()) {
            return group.get();  // if it exists, return it
        }
        Group newGroup = new Group();
        newGroup.setName(name);  // otherwise, create a new group with the specified name

        //        groupRepository.save(newGroup);  // save a new group

        return newGroup;
    }

    // create a list of groups from the reviewGroups permission parameter
    public List<Group> createGroups(List<String> reviewGroups) {
        if (CollectionUtils.isEmpty(reviewGroups)) {
            return null;  // check if the reviewGroups permission exists
        }
        return reviewGroups.stream()  // convert this parameter to a list of groups whose changes the user can accept/reject
                           .map(group -> createGroup(group))
                           .collect(Collectors.toList());
    }
}
