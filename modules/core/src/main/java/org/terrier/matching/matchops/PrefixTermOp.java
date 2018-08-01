package org.terrier.matching.matchops;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.terrier.structures.EntryStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ArrayUtils;

public class PrefixTermOp extends SynonymOp {

	public static final String STRING_PREFIX = "#prefix";
	private static final long serialVersionUID = 1L;
	Predicate<String> predFunction = (e->true);
//	(e ->{ 
//		for (Operator search : terms)
//		{
//			if (e.startsWith(search.toString()))
//			{
//				return true;
//			}
//		}
//		return false; 
//		});
	
	public PrefixTermOp(String searchString) {
		super(new String[]{searchString});
	}
	
	@Override
	public String toString() {
		return STRING_PREFIX + "("+ArrayUtils.join(terms, ' ')+")";
	}
	
	static String getEndString(String termLo)
	{
		return termLo + Character.MAX_VALUE;
	}
	
	@Override
	public Pair<EntryStatistics,IterablePosting> getPostingIterator(Index index) throws IOException
	{
		List<EntryStatistics> _le = new ArrayList<EntryStatistics>();
		List<IterablePosting> _joinedPostings = new ArrayList<IterablePosting>();
		String termLo = ((SingleTermOp)terms[0]).queryTerm;
		String termHi = getEndString(termLo);
		PostingIndex<?> inv = index.getInvertedIndex();
		Iterator<Map.Entry<String,LexiconEntry>> iterLex = index.getLexicon().getLexiconEntryRange(termLo, termHi);
		while(iterLex.hasNext())
		{
			//if (IGNORE_LOW_IDF_TERMS && index.getCollectionStatistics().getNumberOfDocuments() < pair.getKey().getFrequency()) {
			//logger.warn("query term " + ts + " has low idf - ignored from scoring.");
			Map.Entry<String,LexiconEntry> le = iterLex.next(); 
			if (! predFunction.test(le.getKey()))
				continue;
			
			LexiconEntry lee = le.getValue();
			IterablePosting ip = inv.getPostings(lee);
			_le.add(lee);
			_joinedPostings.add(ip);
		}
		
		if (_le.size() == 0)
		{
			//TODO consider if we should return an empty posting list iterator instead
			logger.warn("No alternatives matched in " + Arrays.toString(terms));
			return null;
		}
		EntryStatistics entryStats = mergeStatistics(_le.toArray(new EntryStatistics[_le.size()]), null);
		
		IterablePosting ip = createFinalPostingIterator(_joinedPostings, _le);
		return Pair.of(entryStats, ip);
	}

}
