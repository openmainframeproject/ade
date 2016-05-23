/*
 
    Copyright IBM Corp. 2010, 2016
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
package org.openmainframe.ade.impl.dataStore;

import java.util.Set;
import java.util.TreeSet;

import org.openmainframe.ade.dataStore.IDataStoreDictionaryApi;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.dbUtils.DbDictionary;

public class DataStoreDictionaryApiImpl implements IDataStoreDictionaryApi {

    protected DbDictionary m_dictionary;

    DataStoreDictionaryApiImpl(DbDictionary dictionary) throws AdeException {
        m_dictionary = dictionary;
    }

    @Override
    public void add(String val) throws AdeException {
        m_dictionary.addWord(val);
    }

    @Override
    public Set<String> getAll(boolean refresh) throws AdeException {
        if (refresh) {
            m_dictionary.refresh();
        }
        return new TreeSet<>(m_dictionary.getWords());
    }

    @Override
    public int getMaxLen() {
        return SQL.MAX_LEN_DICTIONARY;
    }

}
