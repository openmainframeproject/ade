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

/**
 * Rule class that contains information for defining a membership rule associated with groups. 
 */
public class Rule {
    /**
     * The unique rule id. 
     */
    private int uid;
    
    /**
     * The unique rule name.
     */
    private String name;
    
    /**
     * The actual rule for membership.
     */
    private String membershipRule;
    
    /**
     * The description of the rule. 
     */
    private String description;
    
    /**
     * Constructor for creating a rule object.
     * @param uid the rule internal id.
     * @param name the rule name.
     * @param membershipRule the actual rule for source membership.
     * @param description an explanation of what the rule means.
     */
    public Rule(int uid, String name, String membershipRule, String description){
        this.uid = uid;
        this.name = name;
        this.membershipRule = membershipRule;
        this.description = description;
    }
    
    /**
     * Constructor for a rule object without an internal id.
     * @param name the rule name.
     * @param membershipRule the actual rule for source membership.
     * @param description an explanation of what the rule maens.
     */
    public Rule(String name, String membershipRule, String description){
        this.name = name;
        this.membershipRule = membershipRule;
        this.description = description;
    }
    
    /**
     * Copy constructor.
     * @param r a rule object to be copied.
     */
    public Rule(Rule r){
        this.uid = r.uid;
        this.name = r.name;
        this.membershipRule = r.membershipRule;
        this.description = r.description;
    }
    
    public final void setUid(int uid) {
        this.uid = uid;
    }
    
    public int getUid(){
        return uid;
    }
    
    public String getName(){
        return name;
    }
    
    public String getMembershipRule(){
        return membershipRule;
    }
    
    public String getDescription(){
        return description;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Rule)) return false;
        Rule rule = (Rule) o;
        return  rule.name.equalsIgnoreCase(name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
