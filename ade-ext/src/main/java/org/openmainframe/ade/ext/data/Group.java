/*
 
    Copyright IBM Corp. 2015, 2016
    This file is part of Anomaly Detection Engine for Linux Logs (ADE).

    ADE is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ADE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ADE.  If not, see <http://www.gnu.org/licenses/>.
 
*/

package org.openmainframe.ade.ext.data;

import org.openmainframe.ade.data.DataType;
import org.openmainframe.ade.data.GroupType;

/**
 * Group class that contains information for defining groups.
 */
public class Group {
    /**
     * The group internal id.
     */
    private int uid;
    /**
     * The group name. Note, the the name is not unique across ALL types of groups. It is only unique only 
     * unique within one group.
     */
    private String name;
    /**
     * The type of the group.
     */
    private GroupType groupType;
    /**
     * The data type that is supported by this group.
     */
    private DataType dataType;
    /**
     * The order this group should be evaluated for membership.
     */
    private int evaluationOrder;
    /**
     * The name of the membership rule associated with this group.
     */
    private String ruleName;

    /**
     * Constructor to create a Group.
     * @param uid The group internal id in the database.
     * @param name the name of the group.
     * @param groupType the type of the group i.e. model
     * @param dataType the type of the data i.e. syslog
     * @param evaluationOrder The order this group should be considered for system membership.
     * @param ruleName the name of the membership rule.
     */
    public Group(int uid, String name, GroupType groupType, DataType dataType, int evaluationOrder, String ruleName) {
        this.uid = uid;
        this.name = name;
        this.groupType = groupType;
        this.dataType = dataType;
        this.evaluationOrder = evaluationOrder;
        this.ruleName = ruleName;
    }
    
    /**
     * Constructor to create a Group without a unique ID.
     * @param name the name of the group.
     * @param groupType the type of the group i.e. model
     * @param dataType the type of the data i.e. syslog
     * @param evaluationOrder the order this group should be considered for system membership
     * @param ruleName the name of the membership rule.
     */    
    public Group(String name, GroupType groupType, DataType dataType, int evaluationOrder, String ruleName) {
        this.name = name;
        this.groupType = groupType;
        this.dataType = dataType;
        this.evaluationOrder = evaluationOrder;
        this.ruleName = ruleName;
    }
    
    /**
     * Group Copy Constructor.
     * @param g group object
     */
    public Group(Group g){
        this.uid = g.uid;
        this.name = g.name;
        this.groupType = g.groupType;
        this.dataType = g.dataType;
        this.evaluationOrder = g.evaluationOrder;
        this.ruleName = g.ruleName;
    }

    public final String getName() {
        return name;
    }
    
    public final GroupType getGroupType() {
        return groupType;
    }
    
    public final DataType getDataType() {
        return dataType;
    }

    public final int getEvaluationOrder() {
        return evaluationOrder;
    }

    public final String getRuleName(){
        return ruleName;
    }

    public final int getUid() {
        return uid;
    }

    public final void setUid(int uid) {
        this.uid = uid;
    }

    public final void setEvaluationOrder(short newOrder) {
        this.evaluationOrder = newOrder;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Group)) return false;
        Group group = (Group) o;
        return  group.name.equalsIgnoreCase(name) && (group.groupType == groupType);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 31 + this.groupType.hashCode();
    }

    @Override
    public String toString() {
        return uid + "/" + name;
    }
}
