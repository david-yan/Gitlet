public class ListNode
{
	public Object		myItem;
	public ListNode	myNext;

	public ListNode(Object item, ListNode next)
	{
		myItem = item;
		myNext = next;
	}
	public ListNode(Object item)
	{
		myItem = item;
		myNext = null;
	}
}
