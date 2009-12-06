/*
 * Copyright (c) 2009 101tec Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package bixo.config;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Set;

import bixo.fetcher.FetchRequest;

@SuppressWarnings("serial")
public class FetcherPolicy implements Serializable {
    public static final int NO_MIN_RESPONSE_RATE = Integer.MIN_VALUE;
    public static final long NO_CRAWL_END_TIME = Long.MIN_VALUE;
    public static final int NO_REDIRECTS = 0;
    
    public static final int DEFAULT_MIN_RESPONSE_RATE = NO_MIN_RESPONSE_RATE;
    public static final int DEFAULT_MAX_CONTENT_SIZE = 64 * 1024;
    public static final long DEFAULT_CRAWL_END_TIME = NO_CRAWL_END_TIME;
    public static final int DEFAULT_MAX_REDIRECTS = 20;
    public static final String DEFAULT_ACCEPT_LANGUAGE = "en-us,en-gb,en;q=0.7,*;q=0.3";
    
    // Interval between batched fetch requests, in milliseconds.
    protected static final long DEFAULT_FETCH_INTERVAL = 5 * 60 * 1000L;
    
    // Interval between requests, in milliseconds.
    protected static final long DEFAULT_CRAWL_DELAY = 30 * 1000L;

    private int _minResponseRate;        // lower bounds on bytes-per-second
    private int _maxContentSize;        // Max # of bytes to use.
    private long _crawlEndTime;          // When we want the crawl to end
    protected long _crawlDelay;            // Delay (in seconds) between requests
    private int _maxRedirects;
    private String _acceptLanguage;    // What to pass for the Accept-Language request header
    private Set<String> _validMimeTypes;    // Set of mime-types that we'll accept, or null
    
    public FetcherPolicy() {
        this(DEFAULT_MIN_RESPONSE_RATE, DEFAULT_MAX_CONTENT_SIZE, DEFAULT_CRAWL_END_TIME, DEFAULT_CRAWL_DELAY, DEFAULT_MAX_REDIRECTS);
    }

    public FetcherPolicy(int minResponseRate, int maxContentSize, long crawlEndTime, long crawlDelay, int maxRedirects) {
        if (crawlDelay < 0) {
            throw new InvalidParameterException("crawlDelay must be >= 0: " + crawlDelay);
        }
        
        // Catch common error of specifying crawl delay in seconds versus milliseconds
        if ((crawlDelay < 100) && (crawlDelay != 0))  {
            throw new InvalidParameterException("crawlDelay must be milliseconds, not seconds: " + crawlDelay);
        }
        
        _minResponseRate = minResponseRate;
        _maxContentSize = maxContentSize;
        _crawlEndTime = crawlEndTime;
        _crawlDelay = crawlDelay;
        _maxRedirects = maxRedirects;
        
        // For rarely used parameters, we'll set it to default values and then let callers set them  individually.
        _acceptLanguage = DEFAULT_ACCEPT_LANGUAGE;
        _validMimeTypes = null;
    }

    /**
     * Calculate the maximum number of URLs that could be processed in the remaining time.
     * 
     * @return Number of URLs
     */
    public int getMaxUrls() {
        if (getCrawlEndTime() == NO_CRAWL_END_TIME) {
            return Integer.MAX_VALUE;
        } else {
            return calcMaxUrls();
        }
    }
    
    public long getDefaultCrawlDelay() {
        return DEFAULT_CRAWL_DELAY;
    }
    
    protected int calcMaxUrls() {
        if (_crawlDelay == 0) {
            return Integer.MAX_VALUE;
        } else {
            long crawlDuration = getCrawlEndTime() - System.currentTimeMillis();
            
            if (crawlDuration <= 0) {
                return 0;
            } else {
                return 1 + (int)Math.max(0, crawlDuration / _crawlDelay);
            }
        }
    }
    
    public long getCrawlEndTime() {
        return _crawlEndTime;
    }

    public void setCrawlEndTime(long crawlEndTime) {
        _crawlEndTime = crawlEndTime;
    }

    public int getMinResponseRate() {
        return _minResponseRate;
    }

    public void setMinResponseRate(int minResponseRate) {
        _minResponseRate = minResponseRate;
    }

    public int getMaxContentSize() {
        return _maxContentSize;
    }

    public void setMaxContentSize(int maxContentSize) {
        _maxContentSize = maxContentSize;
    }
    
    public long getCrawlDelay() {
        return _crawlDelay;
    }
    
    public void setCrawlDelay(long crawlDelay) {
        _crawlDelay = crawlDelay;
    }
    
    public int getMaxRedirects() {
        return _maxRedirects;
    }

    public void setMaxRedirects(int maxRedirects) {
    	_maxRedirects = maxRedirects;
    }
    
    public String getAcceptLanguage() {
        return _acceptLanguage;
    }
    
    public void setAcceptLanguage(String acceptLanguage) {
        _acceptLanguage = acceptLanguage;
    }
    
    public Set<String> getValidMimeTypes() {
        return _validMimeTypes;
    }
    
    public void setValidMimeTypes(Set<String> validMimeTypes) {
        _validMimeTypes = validMimeTypes;
    }
    
    public FetchRequest getFetchRequest(int maxUrls) {
        int numUrls;
        
        if (_crawlDelay > 0) {
            numUrls = Math.min(maxUrls, (int)(DEFAULT_FETCH_INTERVAL / _crawlDelay));
        } else {
            numUrls = maxUrls;
        }
        
        long nextFetchTime = System.currentTimeMillis() + (numUrls * _crawlDelay);
        return new FetchRequest(numUrls, nextFetchTime);
    }
    
    @Override
	public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Crawl end time: " + getCrawlEndTime());
        result.append('\r');
        result.append("Minimum response rate: " + getMinResponseRate());
        result.append('\r');
        result.append("Maximum content size: " + getMaxContentSize());
        result.append('\r');
        result.append("Crawl delay in msec: " + getCrawlDelay());
        result.append('\r');
        result.append("Maximum redirects: " + getMaxRedirects());
        
        return result.toString();
    }
}
