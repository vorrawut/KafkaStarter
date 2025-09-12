# Best Practices 

## Partition should be equal to the number of the consumers

### If partition is more than the number of consumers, the consumers will overwhelm
  ![consumer_group_1](consumer_group_1.png "Consumer Group 1")
  ![consumer_group_4](consumer_group_4.png "Consumer Group 4")
### If partition is evenly distributed, the consumers will not overwhelm (Best Practice)
  ![consumer_group_2](consumer_group_2.png "Consumer Group 2")
### If partition is less than the number of consumers, some consumers will be idle
  ![consumer_group_3](consumer_group_3.png "Consumer Group 3")

## Be careful what you've sent!
- 1 wrong deserialize can cause a claustrophobic to the system, DLQ & Error handling is necessary
