package org.adempiere.exceptions;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class AdempiereExceptionTests
{
	private static final List<Class<?>> boxingExceptionClasses = ImmutableList.<Class<?>> of(ExecutionException.class, InvocationTargetException.class);
	private final Random random = new Random(System.currentTimeMillis());

	@Test
	public void testExtractCauseFrom_ExecutionException()
	{
		final AdempiereException expectedCause = new AdempiereException();

		final Throwable actualCause = AdempiereException.extractCause(new ExecutionException(expectedCause));
		Assert.assertSame(expectedCause, actualCause);
	}

	@Test
	public void testExtractCauseFrom_InvocationTargetException()
	{
		final AdempiereException expectedCause = new AdempiereException();

		final Throwable actualCause = AdempiereException.extractCause(new InvocationTargetException(expectedCause));
		Assert.assertSame(expectedCause, actualCause);
	}

	@Test
	public void testWrapIfNeeded_AdempiereException()
	{
		final AdempiereException expected = new AdempiereException();
		final AdempiereException actual = AdempiereException.wrapIfNeeded(expected);
		Assert.assertSame(expected, actual);
	}

	@Test
	public void testWrapIfNeeded_NotAdempiereException()
	{
		final Throwable rootEx = new RuntimeException("error-" + random.nextInt());
		final AdempiereException actual = AdempiereException.wrapIfNeeded(rootEx);
		Assert.assertNotSame(rootEx, actual);
		Assert.assertSame(rootEx, actual.getCause());
		Assert.assertEquals(rootEx.getLocalizedMessage(), actual.getCause().getLocalizedMessage());
	}

	@Test
	public void testWrapIfNeeded_AdempiereException_Boxed() throws Exception
	{
		final Throwable rootEx = new AdempiereException("error-" + random.nextInt());
		final Throwable rootExBoxed = box(rootEx, 10); // box it 10 times
		final AdempiereException actual = AdempiereException.wrapIfNeeded(rootExBoxed);
		Assert.assertSame(rootEx, actual);
	}

	@Test
	public void testWrapIfNeeded_NotAdempiereException_Boxed() throws Exception
	{
		final Throwable rootEx = new RuntimeException("error-" + random.nextInt());
		final Throwable rootExBoxed = box(rootEx, 10); // box it 10 times
		final AdempiereException actual = AdempiereException.wrapIfNeeded(rootExBoxed);
		Assert.assertNotSame(rootEx, actual);
		Assert.assertNotSame(rootEx, actual);
		Assert.assertSame(rootEx, actual.getCause());
		Assert.assertEquals(rootEx.getLocalizedMessage(), actual.getCause().getLocalizedMessage());
	}

	private final Throwable box(final Throwable throwable, final int depth) throws Exception
	{
		if (depth == 0)
		{
			return throwable;
		}

		final int idx = random.nextInt(boxingExceptionClasses.size());
		final Class<?> exceptionClass = boxingExceptionClasses.get(idx);

		final Throwable throwableBoxed = (Throwable)exceptionClass.getConstructor(Throwable.class).newInstance(throwable);
		return box(throwableBoxed, depth - 1);
	}
}
