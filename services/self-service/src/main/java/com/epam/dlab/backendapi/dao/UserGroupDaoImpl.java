package com.epam.dlab.backendapi.dao;

import com.google.inject.Singleton;

import java.util.Set;

import static com.epam.dlab.backendapi.dao.MongoCollections.USER_GROUPS;
import static com.mongodb.client.model.Filters.eq;

@Singleton
public class UserGroupDaoImpl extends BaseDAO implements UserGroupDao {

	private static final String USERS_FIELD = "users";

	@Override
	public void addUsers(String group, Set<String> users) {
		updateOne(USER_GROUPS, eq(ID, group), addToSet(USERS_FIELD, users), true);
	}

	@Override
	public void removeUser(String group, String user) {
		updateOne(USER_GROUPS, eq(ID, group), pull(USERS_FIELD, user));
	}

	@Override
	public void removeGroup(String groupId) {
		deleteOne(USER_GROUPS, eq(ID, groupId));
	}
}