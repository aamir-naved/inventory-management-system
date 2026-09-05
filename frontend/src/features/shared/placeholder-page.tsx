type PlaceholderPageProps = {
  title: string;
  description: string;
};

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <section className="empty-state">
      <span className="brand-kicker">Coming soon</span>
      <h1>{title}</h1>
      <p>{description}</p>
    </section>
  );
}
