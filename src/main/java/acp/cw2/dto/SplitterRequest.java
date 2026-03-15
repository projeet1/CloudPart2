package acp.cw2.dto;

public class SplitterRequest {

    private String readQueue;
    private String writeTopicOdd;
    private String redisHashOdd;
    private String writeTopicEven;
    private String redisHashEven;
    private int messageCount;

    public SplitterRequest() {
    }

    public String getReadQueue() {
        return readQueue;
    }

    public void setReadQueue(String readQueue) {
        this.readQueue = readQueue;
    }

    public String getWriteTopicOdd() {
        return writeTopicOdd;
    }

    public void setWriteTopicOdd(String writeTopicOdd) {
        this.writeTopicOdd = writeTopicOdd;
    }

    public String getRedisHashOdd() {
        return redisHashOdd;
    }

    public void setRedisHashOdd(String redisHashOdd) {
        this.redisHashOdd = redisHashOdd;
    }

    public String getWriteTopicEven() {
        return writeTopicEven;
    }

    public void setWriteTopicEven(String writeTopicEven) {
        this.writeTopicEven = writeTopicEven;
    }

    public String getRedisHashEven() {
        return redisHashEven;
    }

    public void setRedisHashEven(String redisHashEven) {
        this.redisHashEven = redisHashEven;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}