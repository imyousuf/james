package org.apache.james.imapserver.mock;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.james.imapserver.TestConstants;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

public class MockUsersRepository implements UsersRepository, TestConstants
{

	public boolean addUser(User user)
	{
		throw new RuntimeException("not implemented");
	}

	public void addUser(String name, Object attributes)
	{
		throw new RuntimeException("not implemented");
	}

	public boolean addUser(String username, String password)
	{
		throw new RuntimeException("not implemented");
	}

	public User getUserByName(String name)
	{
		if (USER_NAME.equals(name)) {
			return new MockUser();
		}
		return null;
	}

	public User getUserByNameCaseInsensitive(String name)
	{
		if (USER_NAME.equalsIgnoreCase(name)) {
			return new MockUser();
		}
		return null;
	}

	public String getRealName(String name)
	{
		if (USER_NAME.equalsIgnoreCase(name)) {
			return USER_REALNAME;
		} else {
			return null;
		}
	}

	public boolean updateUser(User user)
	{
		throw new RuntimeException("not implemented");

	}

	public void removeUser(String name)
	{
		throw new RuntimeException("not implemented");

	}

	public boolean contains(String name)
	{
		if (USER_NAME.equals(name)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean containsCaseInsensitive(String name)
	{
		if (USER_NAME.equalsIgnoreCase(name)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean test(String name, String password)
	{
		User user=getUserByName(name);
		if (user!=null) {
			return user.verifyPassword(password);
		}
		return false;
	}

	public int countUsers()
	{
		return 1;
	}

	public Iterator list()
	{
		return Arrays.asList(new String[] { USER_NAME }).iterator();
	}

}
