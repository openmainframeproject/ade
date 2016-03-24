/*
 
    Copyright IBM Corp. 2009, 2016
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
package org.openmainframe.ade.impl.dbUtils;

import java.sql.SQLException;
import java.util.Set;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.utils.NumStringMap;

public class DbDictionary {

    public static final int InvalidID = -1;

    private SQL m_tableName = null;
    private String m_wordColumn;

    NumStringMap m_nameIdMap = null;
    CodeTableSqlStatements m_sqlStatements;
    long m_lastRefreashTime = 0L;

    /**
     * turn this on at your own risk!  This will disable the locking of message id tables in the database.
     * If more then one ade process is active, hell could break lose.  Created to speed things up for the ade_T branch.
     * Matan
     */
    private boolean m_unsafeNoDbLocks = false;

    /**
     * Constructor. Declaring the queries
     * @throws AdeException 
     */
    public DbDictionary(SQL tableName, String idColumn, String wordColumn) throws AdeException {
        m_tableName = tableName;
        m_wordColumn = wordColumn;
        m_sqlStatements = new CodeTableSqlStatements(tableName, idColumn, wordColumn);

        m_unsafeNoDbLocks = Ade.getAde().getConfigProperties().database().UNSAFE_DbNoLocks();

        refresh();
    }

    public int addWord(String word) throws AdeException {
        if (word.length() > SQL.MAX_LEN_DICTIONARY) {
            throw new AdeInternalException("Word too long: " + word);
        }

        int id = m_nameIdMap.getIDFromString(word);
        if (id == NumStringMap.InvalidID) {
            if (!m_unsafeNoDbLocks) {
                id = safelyAddWord(word);
            } else {
                id = unsafelyAddWord(word);
            }
            m_nameIdMap.put(word, id);
        }
        return id;
    }

    public int getDictionarySize() {
        return m_nameIdMap.getMappingCount();
    }

    public int getWordId(String message) {
        return m_nameIdMap.getIDFromString(message);
    }

    public String getWordById(int id) throws AdeException {
        String res = m_nameIdMap.getStringFromID(id);
        if (res != null) {
            return res;
        }
        res = m_sqlStatements.getWord(id);
        if (res == null) {
            throw new AdeInternalException("In " + m_tableName + ": no translation found to " + id);
        }
        m_nameIdMap.put(res, id);
        return res;
    }

    private int safelyAddWord(String word) throws AdeException {

        int id = DbDictionary.InvalidID;
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            cw.startTransaction();
            cw.lockTableShare(m_tableName);
            id = m_sqlStatements.getId(word);
            cw.endTransaction();
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        if (id != DbDictionary.InvalidID) {
            return id;
        }

        try {
            cw.startTransaction();
            cw.lockTableExclusive(m_tableName);
            id = m_sqlStatements.getId(word);
            if (id != DbDictionary.InvalidID) {
                return id;
            }
            // else 
            id = m_sqlStatements.insertWord(word);
            cw.endTransaction();
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();

        }
        return id;
    }

    private int unsafelyAddWord(String word) throws AdeException {

        int id = DbDictionary.InvalidID;
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            cw.startTransaction();
            id = m_sqlStatements.insertWord(word);
            cw.endTransaction();
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return id;
    }

    public Set<Integer> getIds() {
        return m_nameIdMap.getIds();
    }

    public Set<String> getWords() {
        return m_nameIdMap.getWords();
    }

    public void refresh() throws AdeException {
        m_nameIdMap = new NumStringMap(m_sqlStatements.getAllMap().entrySet());
        m_lastRefreashTime = System.currentTimeMillis();
    }

    public String getTableName() {
        return m_tableName.toString();
    }

    public String getWordColumn() {
        return m_wordColumn;
    }

    public String toString() {
        final StringBuilder res = new StringBuilder();
        res.append("[ WordIdDictionary \n");
        for (int id : getIds()) {
            try {
                res.append(String.format("\t%d:%s\n", id, getWordById(id)));
            } catch (AdeException e) {
                throw new Error("Failed converting id", e);
            }
        }
        res.append("]");
        return res.toString();
    }

    public void delete(int id) throws AdeException {
        m_sqlStatements.deleteWord(id);
        m_nameIdMap.removeEntry(id);
    }

    public void clear() {
        m_nameIdMap.clear();

    }

    public long getLastRefreshTime() {
        return m_lastRefreashTime;
    }

}
